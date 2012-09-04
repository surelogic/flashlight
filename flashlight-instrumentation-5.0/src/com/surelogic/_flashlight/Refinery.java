package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.surelogic._flashlight.common.IdConstants;

/**
 * Takes events from the raw queue, refines them, and then places them on the
 * out queue.
 * <P>
 * The refinery tries to identify fields that are accessed only from a single
 * thread during their entire lifetime. If such a field can be identified then
 * events about it are not placed on the out queue. Removal of events is a best
 * effort and some events about the field may be output. These events will have
 * to be removed during data prep.
 */
final class Refinery extends AbstractRefinery {

    private final PostMortemStore f_store;
    private final RunConf f_conf;

    private final BlockingQueue<List<Event>> f_rawQueue;

    private final BlockingQueue<List<Event>> f_outQueue;

    private final DefinitionEventGenerator f_defs;
    /**
     * The desired size of {@link #f_eventCache}.
     */
    private final int f_size;

    // private int filtered = 0, total = 0;

    Refinery(final PostMortemStore store, final RunConf conf,
            final BlockingQueue<List<? extends IdPhantomReference>> gcQueue,
            final BlockingQueue<List<Event>> rawQueue,
            final BlockingQueue<List<Event>> outQueue, final int size) {
        super("flashlight-refinery", gcQueue);
        assert rawQueue != null;
        f_rawQueue = rawQueue;
        assert outQueue != null;
        f_outQueue = outQueue;
        f_size = size;
        f_conf = conf;
        f_store = store;
        f_defs = new DefinitionEventGenerator(conf, outQueue);
        f_refineryStart = System.nanoTime();
    }

    private boolean f_finished = false;

    private final LinkedList<List<Event>> f_eventCache = new LinkedList<List<Event>>();

    private long f_garbageCollectedObjectCount = 0;

    private long f_threadLocalFieldCount = 0;

    private long f_lastRollover = 0;
    private final long f_refineryStart;

    @Override
    public void run() {
        final boolean filter = IdConstants.filterEvents;
        Store.flashlightThread();
        if (StoreConfiguration.debugOn()) {
            System.err.println("Filter events = " + filter);
        }
        int count = 0;
        final List<List<Event>> buf = new ArrayList<List<Event>>();
        while (!f_finished) {
            try {
                List<Event> first = null;
                try {
                    first = f_rawQueue.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Ignored
                    f_conf.logAProblem("Interrupted while calling take()", e);
                    continue;
                }
                if (first != null) {
                    buf.add(first);

                    f_rawQueue.drainTo(buf);

                    /*
                     * System.err.println("Refinery: got "+buf.size()+" lists ("+
                     * num+ ")"); if (buf.size() == 0) { continue; }
                     */
                    for (List<Event> l : buf) {
                        for (Event e : l) {
                            if (e == FinalEvent.FINAL_EVENT) {
                                /*
                                 * We need to delay putting the final event on
                                 * the out queue until all the thread-local
                                 * events get added.
                                 */
                                f_finished = true;
                                break;
                            } else {
                                if (filter) {
                                    e.accept(f_detectSharedFieldsVisitor);
                                }
                            }
                        }
                        f_eventCache.add(l);
                        count += l.size();
                    }
                    buf.clear();
                }
                boolean isCheckpoint = false;
                if (f_finished) {
                    final List<Event> l = f_store.flushLocalQueues();
                    if (filter) {
                        for (Event e : l) {
                            e.accept(f_detectSharedFieldsVisitor);
                        }
                    }
                    f_eventCache.add(l);
                } else {
                    long curTime = System.nanoTime();
                    boolean timesUp;
                    if (f_lastRollover == 0) {
                        timesUp = curTime - f_refineryStart > f_conf
                                .getFileEventInitialDuration();
                    } else {
                        timesUp = curTime - f_lastRollover > f_conf
                                .getFileEventDuration();
                    }
                    if (count > f_conf.getFileEventCount() || timesUp) {
                        final List<Event> l = f_store.flushLocalQueues();
                        if (filter) {
                            for (Event e : l) {
                                e.accept(f_detectSharedFieldsVisitor);
                            }
                        }
                        f_eventCache.add(l);
                        List<Event> cp = new ArrayList<Event>(1);
                        CheckpointEvent ce = new CheckpointEvent(
                                System.nanoTime());
                        cp.add(ce);
                        f_eventCache.add(cp);
                        isCheckpoint = true;
                        count = 0;
                        f_lastRollover = curTime;
                    }
                }

                processGarbageCollectedObjects(filter);
                if (f_finished) {
                    removeRemainingThreadLocalFields();
                }

                final boolean xferd = transferEventsToOutQueue(isCheckpoint);
                if (!xferd) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }
            } catch (IllegalArgumentException e) {
                f_conf.logAProblem("refinery was interrupted...a bug");
            }
        }
        final List<Event> last = new ArrayList<Event>();
        last.add(new Time(new Date(), System.nanoTime()));
        last.add(FinalEvent.FINAL_EVENT);
        PostMortemStore.putInQueue(f_outQueue, last);
        f_conf.log("refinery completed (" + f_garbageCollectedObjectCount
                + " object(s) garbage collected : " + f_threadLocalFieldCount
                + " thread-local fields observed)");
    }

    private final EventVisitor f_detectSharedFieldsVisitor = new EventVisitor() {
        @Override
        void visit(final FieldReadInstance e) {
            visitFieldAccess(e);
        }

        @Override
        void visit(final FieldWriteInstance e) {
            visitFieldAccess(e);
        }

        @Override
        void visit(final FieldReadStatic e) {
            visitFieldAccess(e);
        }

        @Override
        void visit(final FieldWriteStatic e) {
            visitFieldAccess(e);
        }

        private void visitFieldAccess(final FieldAccess e) {
            final IFieldInfo info = e.getFieldInfo();
            final long key = e.getFieldId();
            info.setLastThread(key, e.getWithinThread());
        }
    };

    private final SingleThreadedRefs f_singleThreadedList = new SingleThreadedRefs();

    /**
     * Examines each garbage collected object and cleans up our information
     * about shared fields and thread-local fields.
     * 
     * @param filter
     */
    private void processGarbageCollectedObjects(final boolean filter) {
        List<List<? extends IdPhantomReference>> gcsList = new ArrayList<List<? extends IdPhantomReference>>();
        gcQueue.drainTo(gcsList);
        for (List<? extends IdPhantomReference> gcs : gcsList) {
            final List<Event> events = new ArrayList<Event>();
            final SingleThreadedRefs deadRefs = filter ? new SingleThreadedRefs()
                    : null;
            f_garbageCollectedObjectCount += gcs.size();
            for (IdPhantomReference pr : gcs) {
                if (pr.shouldBeIgnored()) {
                    continue;
                }
                if (filter) {
                    deadRefs.addSingleThreadedObject(pr);
                    removeThreadLocalFieldsWithin(events, deadRefs, pr);
                }
                f_store.gcRWLock(pr);
                events.add(new GarbageCollectedObject(pr));
            }
            if (filter) {
                removeEventsAbout(deadRefs);
            }
            f_eventCache.add(events);
            // System.err.println("Refinery: added a GC list of "+events.size());
        }
    }

    /**
     * Remove all mappings within the {@link #f_fieldToThread} map that are
     * within the garbage collected object.
     * 
     * @param pr
     *            the phantom of the object.
     */
    private void removeThreadLocalFieldsWithin(final List<Event> events,
            final SingleThreadedRefs refs, final PhantomReference pr) {
        /*
         * Collect up all the thread-local fields within the garbage collected
         * object.
         */
        if (pr instanceof ObjectPhantomReference) {
            ObjectPhantomReference obj = (ObjectPhantomReference) pr;
            f_singleThreadedList.clear();

            final boolean added = obj.getFieldInfo().getSingleThreadedFields(
                    f_singleThreadedList);
            if (added) {
                refs.addSingleThreadedFields(f_singleThreadedList
                        .getSingleThreadedFields());
                markAsSingleThreaded(events, f_singleThreadedList);
                f_singleThreadedList.clear();
            }
        }
    }

    private void markAsSingleThreaded(final List<Event> events,
            final SingleThreadedRefs refs) {
        f_threadLocalFieldCount += refs.getSingleThreadedFields().size();
        events.addAll(refs.getSingleThreadedFields());
    }

    /**
     * Removes all <code>FieldAccess</code> events in the event cache about any
     * field in the passed set.
     * 
     * @param fields
     *            the set of fields to remove events about.
     */
    private void removeEventsAbout(final SingleThreadedRefs refs) {
        // final int cacheSize = f_eventCache.size();
        for (List<Event> l : f_eventCache) {
            final int size = l.size();
            for (int i = 0; i < size; i++) {
                Event e = l.get(i);
                if (e instanceof FieldAccess) {
                    // System.out.println("Looking at field access");
                    if (refs.containsField(e)) {
                        // System.out.println("Removed field access");
                        l.set(i, null);
                        /*
                         * filtered++; if ((filtered & 0xff) == 0) {
                         * System.err.println
                         * ("Filtered "+filtered+" out of "+total
                         * +" ("+cacheSize+")"); }
                         */
                    }
                } else if (e instanceof IndirectAccess) {
                    // System.out.println("Looking at indirect access");
                    IndirectAccess a = (IndirectAccess) e;
                    if (refs.containsObject(a.getReceiver())) {
                        // System.out.println("Removed indirect access");
                        l.set(i, null);
                    }
                }
            }
        }
    }

    /**
     * When the refinery finishes up any remaining fields that have been
     * observed to be thread-local can have events about them removed. This
     * method also cleans up the collections used to determine if a field is
     * thread-local or shared.
     */
    private void removeRemainingThreadLocalFields() {
        SingleThreadedRefs refs = ObjectPhantomReference
                .getAllSingleThreadedFields();
        ObservedField.getFieldInfo().getSingleThreadedFields(refs);
        removeEventsAbout(refs);

        final List<Event> events = new ArrayList<Event>();
        markAsSingleThreaded(events, refs);
        f_eventCache.add(events);
    }

    /**
     * Transfers events to the out queue. If we are finished then we add all
     * events, otherwise we just add enough to keep our cache at {@link #f_size}
     * .
     * 
     * @param l
     */
    private boolean transferEventsToOutQueue(final boolean flush) {
        int transferCount = flush || f_finished ? f_eventCache.size()
                : f_eventCache.size() - f_size;
        while (transferCount > 0) {
            final List<Event> buf = f_eventCache.removeFirst();
            for (Event e : buf) {
                if (e instanceof ObjectDefinition) {
                    f_defs.handleDefinition(e);
                }
            }
            transferCount--;
            PostMortemStore.putInQueue(f_outQueue, buf);
        }
        return true;
    }
}
