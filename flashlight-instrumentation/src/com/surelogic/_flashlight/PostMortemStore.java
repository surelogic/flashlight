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
import com.surelogic._flashlight.trace.Traces;

public class PostMortemStore implements StoreListener {

    /**
     * The size of the gcQueue. This governs how many cycles the refinery thread
     * can drift from the {@link GCThread}.
     */
    private static final int GC_DRIFT = 16;

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
    private PostMortemRefinery f_refinery;

    /**
     * The depository thread.
     */
    private Depository f_depository;

    private final UtilConcurrent f_rwLocks;

    private RunConf f_conf;

    public static final class State {
        final ThreadPhantomReference thread;
        public final Traces.Header traceHeader;
        final BlockingQueue<Event> localQueue;

        State() {
            localQueue = new ArrayBlockingQueue<Event>(
                    StoreConfiguration.getRawQueueSize());
            thread = Phantom.ofThread(Thread.currentThread());
            traceHeader = Traces.makeHeader();
        }

        TraceNode getCurrentTrace(final long siteId) {
            return traceHeader.getCurrentNode(this, siteId);
        }

        TraceNode getCurrentTrace() {
            return traceHeader.getCurrentNode();
        }
    }

    /**
     * A State object is kept for each instrumented thread. It is used to store
     * per-thread information needed to support the runtime.
     */
    final ThreadLocal<State> tl_withinStore;

    State createState() {
        State state = new State();
        f_refinery.registerThread(state);
        return state;
    }

    public PostMortemStore() {
        final int outQueueSize = StoreConfiguration.getOutQueueSize();
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

    @Override
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

        putInQueue(f_outQueue, startingEvents);
        IdPhantomReference
        .addObserver(new IdPhantomReferenceCreationObserver() {
            @Override
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
        f_refinery = new PostMortemRefinery(this, f_conf, defs, f_gcQueue,
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

    @Override
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

    @Override
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

    @Override
    public void indirectAccess(final Object receiver, final long siteId) {
        final State state = tl_withinStore.get();
        /*
         * Record this access in the trace.
         */
        putInQueue(state, new IndirectAccess(receiver, siteId, state));
    }

    @Override
    public void arrayAccess(final boolean read, final Object receiver,
            final int index, final long siteId) {
        // Do nothing
    }

    @Override
    public void classInit(final boolean before, final Class<?> clazz) {
        // Do nothing
    }

    @Override
    public void constructorCall(final boolean before, final long siteId) {
        State state = tl_withinStore.get();
        if (before) {
            state.traceHeader.pushTraceNode(state, siteId);
        } else {
            state.traceHeader.popTraceNode();
        }
    }

    @Override
    public void constructorExecution(final boolean before,
            final Object receiver, final long siteId) {
        // Do nothing
    }

    @Override
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
            state.traceHeader.pushTraceNode(state, siteId);
        } else {
            state.traceHeader.popTraceNode();
        }

    }

    @Override
    public void beforeIntrinsicLockAcquisition(final Object lockObject,
            final boolean lockIsThis, final long siteId) {
        State state = tl_withinStore.get();
        final Event e = new BeforeIntrinsicLockAcquisition(lockObject,
                lockIsThis, siteId, state);
        putInQueue(state, e);
    }

    @Override
    public void afterIntrinsicLockAcquisition(final Object lockObject,
            boolean lockIsThis, final long siteId) {
        State state = tl_withinStore.get();
        final Event e = new AfterIntrinsicLockAcquisition(lockObject, siteId,
                state, lockIsThis);
        putInQueue(state, e);
    }

    @Override
    public void intrinsicLockWait(final boolean before,
            final Object lockObject, boolean lockIsThis, final long siteId) {
        final Event e;
        State state = tl_withinStore.get();
        if (before) {
            e = new BeforeIntrinsicLockWait(lockObject, siteId, state,
                    lockIsThis);
        } else {
            e = new AfterIntrinsicLockWait(lockObject, siteId, state,
                    lockIsThis);
        }
        putInQueue(state, e);

    }

    @Override
    public void afterIntrinsicLockRelease(final Object lockObject,
            boolean lockIsThis, final long siteId) {
        State state = tl_withinStore.get();
        final Event e = new AfterIntrinsicLockRelease(lockObject, siteId,
                state, lockIsThis);
        putInQueue(state, e);
    }

    @Override
    public void beforeUtilConcurrentLockAcquisitionAttempt(
            final Object lockObject, final long siteId) {
        if (lockObject instanceof Lock) {
            State state = tl_withinStore.get();
            final Lock ucLock = (Lock) lockObject;
            final Event e = new BeforeUtilConcurrentLockAcquisitionAttempt(
                    ucLock, siteId, state);
            putInQueue(state, e);
        } else {
            final String fmt = "lock object must be a java.util.concurrent.locks.Lock...instrumentation bug detected by Store.beforeUtilConcurrentLockAcquisitionAttempt(lockObject=%s, location=%s)";
            f_conf.logAProblem(String.format(fmt, lockObject, siteId));
            return;
        }
    }

    @Override
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

    @Override
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

    @Override
    public void instanceFieldInit(final Object receiver, final int fieldId,
            final Object value) {
        putInQueue(tl_withinStore.get(), new FieldAssignment(receiver, fieldId,
                value));
    }

    @Override
    public void staticFieldInit(final int fieldId, final Object value) {
        putInQueue(tl_withinStore.get(), new FieldAssignment(fieldId, value));
    }

    @Override
    public void shutdown() {
        /*
         * Finish up data output.
         */
        Thread.yield();
        f_refinery.requestShutdown();
        join(f_refinery);
        final List<Event> last = new ArrayList<Event>();
        last.add(new Time(new Date(), System.nanoTime()));
        last.add(FinalEvent.FINAL_EVENT);
        putInQueue(f_outQueue, last);
        join(f_depository);
    }

    static final int LOCAL_QUEUE_MAX = 256;

    public static void putInQueue(final State state, final Event e) {
        putInQueue(state.localQueue, e);
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
        boolean interrupted = false;
        while (!done) {
            try {
                queue.put(e);
                done = true;
            } catch (final InterruptedException e1) {
                /*
                 * We are within a program thread, so another program thread
                 * interrupted us. We ensure the event still gets put into the
                 * raw queue, then appropriately set the interrupted status.
                 */
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
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

    @Override
    public Collection<? extends ConsoleCommand> getCommands() {
        return Collections.emptyList();
    }

    @Override
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

    @Override
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

    @Override
    public void happensBeforeThread(String id, Thread callee, long siteId,
            String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state, new HappensBeforeThread(id, Phantom.ofThread(callee),
                siteId, state, nanoTime));
    }

    @Override
    public void happensBeforeObject(String id, Object object, long siteId,
            String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state, new HappensBeforeObject(id, Phantom.ofObject(object),
                siteId, state, nanoTime));
    }

    @Override
    public void happensBeforeCollection(String id, Object collection,
            Object item, long siteId, String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state,
                new HappensBeforeCollection(id, Phantom.ofObject(collection),
                        item == null ? null : Phantom.ofObject(item), siteId,
                                state, nanoTime));
    }

    @Override
    public void happensBeforeExecutor(String id, Object object, long siteId,
            String typeName, long nanoTime) {
        State state = tl_withinStore.get();
        putInQueue(state,
                new HappensBeforeExecutor(id, Phantom.ofObject(object), siteId,
                        state, nanoTime));
    }

}
