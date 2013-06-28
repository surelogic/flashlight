package com.surelogic._flashlight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.surelogic._flashlight.Store.GCThread;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic._flashlight.trace.TraceNode;

public class PostMortemStore implements StoreListener {

    /**
     * The size of the gcQueue. This governs how many cycles the refinery thread
     * can drift from the {@link GCThread}.
     */
    private static final int GC_DRIFT = 16;

    /**
     * A queue to buffer raw event records from the instrumented program to the
     * {@link Refinery}.
     */
    private final BlockingQueue<List<Event>> f_rawQueue;

    /**
     * A queue to buffer refined event records from the {@link Refinery} to the
     * {@link Depository}.
     */
    private final BlockingQueue<List<Event>> f_outQueue;
    /**
     * The garbage collection queue.
     */
    private final BlockingQueue<List<? extends IdPhantomReference>> f_gcQueue;

    /**
     * The refinery thread.
     */
    private AbstractRefinery f_refinery;

    /**
     * The depository thread.
     */
    private Depository f_depository;

    private final UtilConcurrent f_rwLocks;

    private RunConf f_conf;

    public static final class State {
        final ThreadPhantomReference thread;
        public final TraceNode.Header traceHeader;
        final List<Event> eventQueue;
        final BlockingQueue<List<Event>> rawQueue;

        State(final BlockingQueue<List<Event>> q, final List<Event> l) {
            rawQueue = q;
            eventQueue = l;
            thread = Phantom.ofThread(Thread.currentThread());
            traceHeader = TraceNode.makeHeader();
        }

        TraceNode getCurrentTrace(final long siteId) {
            return traceHeader.getCurrentNode(siteId, this);
        }

        TraceNode getCurrentTrace() {
            return traceHeader.getCurrentNode(this);
        }
    }

    /**
     * A State object is kept for each instrumented thread. It is used to store
     * per-thread information needed to support the runtime.
     */
    final ThreadLocal<State> tl_withinStore;

    State createState() {
        final List<Event> l = new ArrayList<Event>(LOCAL_QUEUE_MAX);
        synchronized (localQueueList) {
            localQueueList.add(l);
        }
        return new State(f_rawQueue, l);
    }

    public PostMortemStore() {
        final int rawQueueSize = StoreConfiguration.getRawQueueSize();
        final int outQueueSize = StoreConfiguration.getOutQueueSize();
        f_rawQueue = new ArrayBlockingQueue<List<Event>>(rawQueueSize);
        f_outQueue = new ArrayBlockingQueue<List<Event>>(outQueueSize);
        f_gcQueue = new ArrayBlockingQueue<List<? extends IdPhantomReference>>(
                GC_DRIFT);

        f_rwLocks = new UtilConcurrent();

        tl_withinStore = new ThreadLocal<PostMortemStore.State>() {
            @Override
            protected State initialValue() {
                return createState();
            }
        };

    }

    public void init(final RunConf conf) {
        f_conf = conf;
        // Create starting events
        Environment environmentEvent = new Environment();
        Time timeEvent = new Time(conf.getStartTime(), conf.getStartNanoTime());
        List<Event> startingEvents = new ArrayList<Event>(2);
        startingEvents.add(environmentEvent);
        startingEvents.add(timeEvent);

        // Initialize Refinery and Depository
        final OutputType outType = StoreConfiguration.getOutputType();
        if (StoreConfiguration.debugOn()) {
            System.err.println("Output XML = " + outType);
        }
        EventVisitor outputStrategy = null;
        if (StoreConfiguration.debugOn()) {
            System.err.println("Compress stream = " + outType.isCompressed());
        }
        final EventVisitor.Factory factory = OutputStrategyXML.factory;
        if (StoreConfiguration.hasOutputPort()) {
            // This check needs to be before the MultiFileOutput check,
            // as we do not switch output streams when we are using
            // checkpointing and sockets at the same time.
            f_conf.log("Using network output.");
            outputStrategy = new SocketOutputStrategy(f_conf, factory, outType);
        } else {
            f_conf.log("Using checkpointing output.");
            outputStrategy = new CheckpointingOutputStreamStrategy(f_conf,
                    factory, outType);
        }

        // Initialize Queues

        putInQueue(f_rawQueue, startingEvents);
        IdPhantomReference
                .addObserver(new IdPhantomReferenceCreationObserver() {
                    public void notify(final ClassPhantomReference o,
                            final IdPhantomReference r) {
                        /*
                         * Create an event to define this object.
                         */
                        putInQueue(tl_withinStore.get(), new ObjectDefinition(
                                o, r));
                    }
                });
        DefinitionEventGenerator defs = new DefinitionEventGenerator(conf,
                f_outQueue);
        // Start Refinery and Depository
        final int refinerySize = StoreConfiguration.getRefinerySize();
        f_refinery = new Refinery(this, f_conf, defs, f_gcQueue, f_rawQueue,
                f_outQueue, refinerySize);
        f_refinery.start();
        f_depository = new Depository(f_conf, f_outQueue, outputStrategy);
        f_depository.start();
        f_conf.log("collection started (rawQ="
                + StoreConfiguration.getRawQueueSize() + " : refinery="
                + refinerySize + " : outQ="
                + StoreConfiguration.getOutQueueSize() + ")");
        f_conf.logFlush();
    }

    public void instanceFieldAccess(final boolean read, final Object receiver,
            final int fieldID, final long siteId,
            final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
        final State state = tl_withinStore.get();
        final Event e;
        if (read) {
            e = new FieldReadInstance(receiver, fieldID, siteId, state);
        } else {
            e = new FieldWriteInstance(receiver, fieldID, siteId, state);
        }
        putInQueue(state, e);
    }

    public void staticFieldAccess(final boolean read, final int fieldID,
            final long siteId, final ClassPhantomReference dcPhantom,
            final Class<?> declaringClass) {
        boolean underConstruction = false;
        final State state = tl_withinStore.get();
        if (dcPhantom != null) {
            underConstruction = dcPhantom.isUnderConstruction();
        }
        final Event e;
        if (read) {
            e = new FieldReadStatic(fieldID, siteId, state, underConstruction);
        } else {
            e = new FieldWriteStatic(fieldID, siteId, state, underConstruction);
        }
        putInQueue(state, e);
    }

    public void indirectAccess(final Object receiver, final long siteId) {
        final State state = tl_withinStore.get();
        /*
         * Record this access in the trace.
         */
        putInQueue(state, new IndirectAccess(receiver, siteId, state));
    }

    public void arrayAccess(final boolean read, final Object receiver,
            final int index, final long siteId) {
        // Do nothing
    }

    public void classInit(final boolean before, final Class<?> clazz) {
        // Do nothing
    }

    public void constructorCall(final boolean before, final long siteId) {
        State state = tl_withinStore.get();
        if (before) {
            TraceNode.pushTraceNode(siteId, state);
        } else {
            TraceNode.popTraceNode(siteId, state);
        }
    }

    public void constructorExecution(final boolean before,
            final Object receiver, final long siteId) {
        // Do nothing
    }

    public void methodCall(final boolean before, final Object receiver,
            final long siteId) {

        State state = tl_withinStore.get();

        /*
         * Special handling for ReadWriteLocks
         */
        if (receiver instanceof ReadWriteLock) {
            /*
             * Define the structure of the ReadWriteLock in an event.
             */
            final ReadWriteLock rwl = (ReadWriteLock) receiver;
            final ObjectPhantomReference p = Phantom.ofObject(rwl);
            if (f_rwLocks.addReadWriteLock(p)) {
                if (f_conf.isDebug()) {
                    final String fmt = "Defined ReadWriteLock id=%d";
                    f_conf.log(String.format(fmt, p.getId()));
                }
                final Event e = new ReadWriteLockDefinition(p,
                        Phantom.ofObject(rwl.readLock()), Phantom.ofObject(rwl
                                .writeLock()));
                putInQueue(state, e);
            }
        }

        /*
         * Record this call in the trace.
         */
        if (before) {
            TraceNode.pushTraceNode(siteId, state);
        } else {
            TraceNode.popTraceNode(siteId, state);
        }

    }

    public void beforeIntrinsicLockAcquisition(final Object lockObject,
            final boolean lockIsThis, final boolean lockIsClass,
            final long siteId) {
        State state = tl_withinStore.get();
        final Event e = new BeforeIntrinsicLockAcquisition(lockObject,
                lockIsThis, lockIsClass, siteId, state);
        putInQueue(state, e, true);
    }

    public void afterIntrinsicLockAcquisition(final Object lockObject,
            final long siteId) {
        State state = tl_withinStore.get();
        final Event e = new AfterIntrinsicLockAcquisition(lockObject, siteId,
                state);
        putInQueue(state, e);
    }

    public void intrinsicLockWait(final boolean before,
            final Object lockObject, final long siteId) {
        final Event e;
        State state = tl_withinStore.get();
        if (before) {
            e = new BeforeIntrinsicLockWait(lockObject, siteId, state);
        } else {
            e = new AfterIntrinsicLockWait(lockObject, siteId, state);
        }
        putInQueue(state, e, true);

    }

    public void afterIntrinsicLockRelease(final Object lockObject,
            final long siteId) {
        State state = tl_withinStore.get();
        final Event e = new AfterIntrinsicLockRelease(lockObject, siteId, state);
        putInQueue(state, e);
    }

    public void beforeUtilConcurrentLockAcquisitionAttempt(
            final Object lockObject, final long siteId) {
        if (lockObject instanceof Lock) {
            State state = tl_withinStore.get();
            final Lock ucLock = (Lock) lockObject;
            final Event e = new BeforeUtilConcurrentLockAcquisitionAttempt(
                    ucLock, siteId, state);
            putInQueue(state, e, true);
        } else {
            final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.beforeUtilConcurrentLockAcquisitionAttempt(lockObject=%s, location=%s)";
            f_conf.logAProblem(String.format(fmt, lockObject, siteId));
            return;
        }
    }

    public void afterUtilConcurrentLockAcquisitionAttempt(
            final boolean gotTheLock, final Object lockObject, final long siteId) {
        if (lockObject instanceof Lock) {
            State state = tl_withinStore.get();
            final Lock ucLock = (Lock) lockObject;
            final Event e = new AfterUtilConcurrentLockAcquisitionAttempt(
                    gotTheLock, ucLock, siteId, state);
            putInQueue(state, e);
        } else {
            final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.afterUtilConcurrentLockAcquisitionAttempt(%s, lockObject=%s, location=%s)";
            f_conf.logAProblem(String.format(fmt, gotTheLock ? "holding"
                    : "failed-to-acquire", lockObject, siteId));
            return;
        }

    }

    public void afterUtilConcurrentLockReleaseAttempt(
            final boolean releasedTheLock, final Object lockObject,
            final long siteId) {
        if (lockObject instanceof Lock) {
            State state = tl_withinStore.get();
            final Lock ucLock = (Lock) lockObject;
            final Event e = new AfterUtilConcurrentLockReleaseAttempt(
                    releasedTheLock, ucLock, siteId, state);
            putInQueue(state, e);
        } else {
            final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.afterUtilConcurrentLockReleaseAttempt(%s, lockObject=%s, location=%s)";
            f_conf.logAProblem(String.format(fmt, releasedTheLock ? "released"
                    : "failed-to-release", lockObject, siteId));
            return;
        }
    }

    public void instanceFieldInit(final Object receiver, final int fieldId,
            final Object value) {
        putInQueue(tl_withinStore.get(), new FieldAssignment(receiver, fieldId,
                value));
    }

    public void staticFieldInit(final int fieldId, final Object value) {
        putInQueue(tl_withinStore.get(), new FieldAssignment(fieldId, value));
    }

    public void shutdown() {
        /*
         * Finish up data output.
         */
        Thread.yield();
        putInQueue(f_rawQueue, flushLocalQueues());
        final List<Event> last = new ArrayList<Event>();
        last.add(new Time(new Date(), System.nanoTime()));
        last.add(FinalEvent.FINAL_EVENT);
        putInQueue(f_rawQueue, last);
        join(f_refinery);
        join(f_depository);
    }

    static final int LOCAL_QUEUE_MAX = 256;

    /**
     * Used by the refinery to flush all the local queues upon shutdown
     */
    final List<List<Event>> localQueueList = new ArrayList<List<Event>>();

    public static void putInQueue(final State state, final Event e) {
        putInQueue(state, e, false);
    }

    static void putInQueue(final State state, final Event e, final boolean flush) {

        final List<Event> localQ = state.eventQueue;
        List<Event> copy = null;
        synchronized (localQ) {
            localQ.add(e);
            if (/* flush || */localQ.size() >= LOCAL_QUEUE_MAX) {
                copy = new ArrayList<Event>(localQ);
                localQ.clear();
            }
        }
        if (copy != null) {
            putInQueue(state.rawQueue, copy);
        }
    }

    /**
     * Puts an event into a blocking queue. This operation will block if the
     * queue is full and it will ignore any interruptions.
     * 
     * @param queue
     *            the blocking queue to put the event into.
     * @param e
     *            the event to put into the raw queue.
     */
    static <T> void putInQueue(final BlockingQueue<T> queue, final T e) {
        boolean done = false;
        while (!done) {
            try {
                queue.put(e);
                done = true;
            } catch (final InterruptedException e1) {
                /*
                 * We are within a program thread, so another program thread
                 * interrupted us. I think it is OK to ignore this, however, we
                 * do need to ensure the event gets put into the raw queue.
                 */
                // f_conf.logAProblem("queue.put(e) was interrupted", e1);
            }
        }
    }

    /**
     * Clear all of the local queues. This must be called before a checkpoint
     * event, in order to ensure that we see the entirety of the program in a
     * consistent state. Without flushing local queues, events on
     * low-computation threads may sit around for a very long time compared to
     * high-computation threads.
     * 
     * @return
     */
    List<Event> flushLocalQueues() {
        final List<Event> buf = new ArrayList<Event>(LOCAL_QUEUE_MAX);
        synchronized (localQueueList) {
            flushLocalQueuesHelper(0, localQueueList, localQueueList.size(),
                    buf);
        }
        return buf;
    }

    /*
     * Recursively acquire every lock in the list of local queues. A lock on
     * queueList must be acquired before this method can be called.
     */
    private void flushLocalQueuesHelper(final int index,
            final List<List<Event>> queueList, final int queueLength,
            final List<Event> output) {
        if (index == queueLength) {
            flushAllQueues(queueList, output);
        } else {
            synchronized (queueList.get(index)) {
                flushLocalQueuesHelper(index + 1, queueList, queueLength,
                        output);
            }
        }
    }

    /*
     * A lock must already be held on all queues before this method is called.
     */
    private void flushAllQueues(final List<List<Event>> queueList,
            final List<Event> output) {
        // We drain the raw queue here too, just in case events have crept back
        // in since the last time the refinery drained it. We do this first, so
        // as to preserve the order of events in the same thread.
        final List<List<Event>> rawEvents = new ArrayList<List<Event>>();
        f_rawQueue.drainTo(rawEvents);
        for (final List<Event> e : rawEvents) {
            output.addAll(e);
        }
        for (final List<Event> q : queueList) {
            output.addAll(q);
            q.clear();
        }
    }

    /**
     * Joins on the given thread ignoring any interruptions.
     * 
     * @param t
     *            the thread to join on.
     */
    private static void join(final Thread t) {
        if (t == null) {
            return;
        }
        boolean done = false;
        while (!done) {
            try {
                t.join();
                done = true;
            } catch (final InterruptedException e1) {
                // ignore, we expect to be interrupted
            }
        }
    }

    /**
     * Used by the refinery, gets the {@link State} of the current thread.
     * 
     * @return
     */
    public State getState() {
        return tl_withinStore.get();
    }

    /**
     * Used by the refinery to notify the store of when a read/write lock has
     * been garbage collected.
     * 
     * @param pr
     */
    public void gcRWLock(final IdPhantomReference pr) {
        f_rwLocks.remove(pr);
    }

    public Collection<? extends ConsoleCommand> getCommands() {
        return Collections.emptyList();
    }

    public void garbageCollect(
            final List<? extends IdPhantomReference> references) {
        for (;;) {
            try {
                f_gcQueue.put(new ArrayList<IdPhantomReference>(references));
                return;
            } catch (InterruptedException e) {
                // Just try again
            }
        }
    }

    public void methodExecution(boolean before, long siteId) {

        /*
         * Record this call in the trace.
         */
        /*
         * State state = tl_withinStore.get(); if (before) {
         * TraceNode.pushTraceNode(siteId, state); } else {
         * TraceNode.popTraceNode(siteId, state); }
         */
    }

    public void happensBeforeThread(Thread callee, long siteId,
            String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state, new HappensBeforeThread(Phantom.ofThread(callee),
                siteId, state, nanoTime));
    }

    public void happensBeforeObject(Object object, long siteId,
            String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state, new HappensBeforeObject(Phantom.ofObject(object),
                siteId, state, nanoTime));
    }

    public void happensBeforeCollection(Object collection, Object item,
            long siteId, String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state,
                new HappensBeforeCollection(Phantom.ofObject(collection),
                        item == null ? null : Phantom.ofObject(item), siteId,
                        state, nanoTime));
    }

}
