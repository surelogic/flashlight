package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PostMortemRefinery extends Thread {

    private final PostMortemStore f_store;
    private final RunConf f_conf;
    private final DefinitionEventGenerator f_defs;
    private final BlockingQueue<StateReference> f_toRegister;
    private final List<StateReference> activeThreads;
    private final List<Event> f_eventCache;
    private final int f_eventCacheSize;

    private long f_lastRollover;
    private final long f_refineryStart;
    private long f_threadLocalFieldCount;

    private final BlockingQueue<List<? extends IdPhantomReference>> f_gcQueue;
    private long f_garbageCollectedObjectCount;

    private final BlockingQueue<List<Event>> f_outQueue;

    private final SingleThreadedRefs f_singleThreadedList = new SingleThreadedRefs();

    private volatile boolean f_shutdown;

    public PostMortemRefinery(PostMortemStore store, RunConf conf,
            DefinitionEventGenerator defs,
            BlockingQueue<List<? extends IdPhantomReference>> gcQueue,
            BlockingQueue<List<Event>> outQueue, int refinerySize) {
        super("flashlight-refinery");
        f_store = store;
        f_conf = conf;
        f_defs = defs;
        f_shutdown = false;
        f_toRegister = new ArrayBlockingQueue<PostMortemRefinery.StateReference>(
                StoreConfiguration.getThreadRegistrationQueueSize());
        activeThreads = new LinkedList<PostMortemRefinery.StateReference>();
        f_eventCache = new ArrayList<Event>();
        f_eventCacheSize = refinerySize;
        f_refineryStart = System.nanoTime();
        f_gcQueue = gcQueue;
        f_outQueue = outQueue;
    }

    void requestShutdown() {
        f_shutdown = true;
    }

    void registerThread(PostMortemStore.State state) {
        PostMortemStore.putInQueue(f_toRegister, new StateReference(state));
    }

    @Override
    public void run() {
        int count = 0;
        for (;;) {
            boolean isFinished = f_shutdown;
            // Add any threads that need to be registered to the list of active
            // threads.
            f_toRegister.drainTo(activeThreads);

            long curTime = System.nanoTime();
            boolean timesUp;
            if (f_lastRollover == 0) {
                timesUp = curTime - f_refineryStart > f_conf
                        .getFileEventInitialDuration();
            } else {
                timesUp = curTime - f_lastRollover > f_conf
                        .getFileEventDuration();
            }
            boolean isCheckpoint = count > f_conf.getFileEventCount()
                    || timesUp;
            if (isCheckpoint) {
                /*
                 * place all events preceding the checkpoint time into the event
                 * cache before the checkpoint event
                 */
                List<Event> buf = new ArrayList<Event>();
                for (Iterator<StateReference> iter = activeThreads.iterator(); iter
                        .hasNext();) {
                    StateReference ref = iter.next();
                    boolean isThreadDone = ref.get() == null;
                    ref.localQueue.drainTo(buf);
                    if (isThreadDone) {
                        iter.remove();
                    }
                }
                for (Event e : buf) {
                    if (!e.isTimedEvent() || e.getTime() <= curTime) {
                        f_eventCache.add(e);
                    }
                }
                f_eventCache.add(new CheckpointEvent(curTime));
                for (Event e : buf) {
                    if (e.isTimedEvent() && e.getTime() > curTime) {
                        f_eventCache.add(e);
                    }
                }
                count = 0;
                f_lastRollover = curTime;
            } else {
                /*
                 * Not a checkpoint
                 */
                for (Iterator<StateReference> iter = activeThreads.iterator(); iter
                        .hasNext();) {
                    StateReference ref = iter.next();
                    boolean isThreadDone = ref.get() == null;
                    count += ref.localQueue.drainTo(f_eventCache);
                    if (isThreadDone) {
                        iter.remove();
                    }
                }
            }
            // We are done here, so shut down the thread
            processGarbageCollectedObjects();
            if (isFinished) {
                removeRemainingThreadLocalFields();
            }
            boolean xferd = transferEventsToOutQueue(isCheckpoint || isFinished);
            if (isFinished) {
                break;
            } else if (xferd) {
                /*
                 * This prevents us from busy looping when there is no data.
                 */
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        f_conf.log("refinery completed (" + f_garbageCollectedObjectCount
                + " object(s) garbage collected : " + f_threadLocalFieldCount
                + " thread-local fields observed)");
    }

    /**
     * Examines each garbage collected object and cleans up our information
     * about shared fields and thread-local fields.
     *
     * @param filter
     */
    private void processGarbageCollectedObjects() {
        List<List<? extends IdPhantomReference>> gcsList = new ArrayList<List<? extends IdPhantomReference>>();
        f_gcQueue.drainTo(gcsList);
        for (List<? extends IdPhantomReference> gcs : gcsList) {
            final List<Event> events = new ArrayList<Event>();
            final SingleThreadedRefs deadRefs = new SingleThreadedRefs();
            f_garbageCollectedObjectCount += gcs.size();
            for (IdPhantomReference pr : gcs) {
                if (pr.isDuplicate()) {
                    continue;
                }
                // New version
                if (pr instanceof ObjectPhantomReference) {
                    if (((ObjectPhantomReference) pr).sharedByThreads()) {
                        deadRefs.addSingleThreadedObject(pr);
                    }
                }
                removeThreadLocalFieldsWithin(events, deadRefs, pr);
                f_store.gcRWLock(pr);
                events.add(new GarbageCollectedObject(pr));
            }
            removeEventsAbout(deadRefs);
            f_eventCache.addAll(events);
            // System.err.println("Refinery: added a GC list of "+events.size());
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
        f_eventCache.addAll(events);
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
        final int size = f_eventCache.size();
        for (int i = 0; i < size; i++) {
            Event e = f_eventCache.get(i);
            if (e instanceof FieldAccess) {
                if (refs.containsField(e)) {
                    f_eventCache.set(i, null);
                }
            } else if (e instanceof IndirectAccess) {
                IndirectAccess a = (IndirectAccess) e;
                if (refs.containsObject(a.getReceiver())) {
                    f_eventCache.set(i, null);
                }
            }
        }
    }

    /**
     * Transfers events to the out queue. If we are finished then we add all
     * events, otherwise we just add enough to keep our cache at {@link #f_size}
     * .
     *
     * @param l
     */
    private boolean transferEventsToOutQueue(final boolean flush) {
        int transferCount = flush ? f_eventCache.size() : f_eventCache.size()
                - f_eventCacheSize;
        if (transferCount > 0) {
            List<Event> toTransfer = f_eventCache.subList(0, transferCount);
            for (Event e : toTransfer) {
                if (e instanceof ObjectDefinition) {
                    f_defs.handleDefinition(e);
                }
            }
            PostMortemStore.putInQueue(f_outQueue, new ArrayList<Event>(
                    toTransfer));
            toTransfer.clear();
            return true;
        } else {
            return false;
        }
    }

    /**
     * We keep a weak reference to the state in order to allow it to be garbage
     * collected. The local queue is kept on the reference explicitly in order
     * to allow any final events in the thread to be cleared.
     *
     * @author nathan
     *
     */
    private static class StateReference extends
            WeakReference<PostMortemStore.State> {

        final BlockingQueue<Event> localQueue;

        public StateReference(
                com.surelogic._flashlight.PostMortemStore.State referent) {
            super(referent);
            localQueue = referent.localQueue;
        }

    }

}
