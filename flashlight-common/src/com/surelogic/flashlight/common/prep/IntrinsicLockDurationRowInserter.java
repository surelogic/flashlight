package com.surelogic.flashlight.common.prep;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.JDBCUtils;
import com.surelogic.common.logging.SLLogger;

public final class IntrinsicLockDurationRowInserter {

    private static final int EDGE_HINT = 3;
    private static final EdgeFactory EDGE_FACTORY = new EdgeFactory();

    private static final long FINAL_EVENT = Lock.FINAL_EVENT;
    private static final int LOCK_DURATION = 0;
    private static final int LOCKS_HELD = 1;
    private static final int LOCK_CYCLE = 2;
    private static final int INSERT_LOCK = 3;
    private static final boolean doInsert = AbstractPrep.doInsert;

    private static final String[] queries = {
            "INSERT INTO LOCKDURATION (InThread,Lock,Start,StartEvent,Stop,StopEvent,Duration,State) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            "INSERT INTO LOCKSHELD (LockEvent,LockHeldEvent,LockHeld,LockAcquired,InThread) VALUES (?, ?, ?, ?, ?)",
            "INSERT INTO LOCKCYCLE (Component,LockHeld,LockAcquired,Count,FirstTime,LastTime) VALUES (?, ?, ?, ?, ?, ?)",
            "INSERT INTO LOCK (Id,TS,InThread,Trace,Lock,Object,Type,State,Success,LockIsThis,LockIsClass) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" };
    private final PreparedStatement[] statements = new PreparedStatement[queries.length];
    private final int[] counts = new int[queries.length];
    private final Calendar here = new GregorianCalendar();
    private final Logger log = SLLogger
            .getLoggerFor(IntrinsicLockDurationRowInserter.class);

    static class State {
        IntrinsicLockDurationState _lockState = IntrinsicLockDurationState.IDLE;
        int _timesEntered = 0;

        // Info about the event that started us in this state
        long _id;
        long _lock;
        long _object;
        long _trace;
        Timestamp _time;
        LockState _startEvent;

        public IntrinsicLockDurationState getLockState() {
            return _lockState;
        }

        public int getTimesEntered() {
            return _timesEntered;
        }

        public long getId() {
            return _id;
        }

        public long getLock() {
            return _lock;
        }

        public long getTrace() {
            return _trace;
        }

        public Timestamp getTime() {
            return _time;
        }

        public LockState getStartEvent() {
            return _startEvent;
        }

        public void lockReleased() {
            --_timesEntered;
        }

        public void lockAcquired() {
            ++_timesEntered;
        }

        public long getLockObject() {
            return _object;
        }

    }

    static class ThreadState {
        TLongObjectMap<State> lockToState = new TLongObjectHashMap<State>();
        final Set<State> nonIdleLocks = new HashSet<State>();
        final List<State> heldLocks = new ArrayList<State>();

        /**
         * Iterate over the set of non-idle locks
         * 
         * @return
         */
        public Collection<State> nonIdleLocks() {
            return nonIdleLocks;
        }

        /**
         * Called when a lock is garbage collected.
         * 
         * @param key
         */
        public void gcLock(final Long key) {
            final State state = lockToState.remove(key);
            if (state != null) {
                nonIdleLocks.remove(state);
                heldLocks.remove(state);
            }
        }

        /**
         * Return the state associated with the given lock in this thread.
         * 
         * @param lock
         * @return
         */
        public State getState(final long lock) {
            State event = lockToState.get(lock);
            if (event == null) {
                event = new State();
                event._lock = lock;
                lockToState.put(lock, event);
            }
            return event;
        }

        /**
         * Get the set of locks that are currently held.
         * 
         * @return
         */
        public List<State> heldLocks() {
            return heldLocks;
        }

        public void update(final State mutableState, final long id,
                final long trace, final long object, final Timestamp time,
                final LockState startEvent,
                final IntrinsicLockDurationState lockState) {
            if (mutableState.getLockState() == IntrinsicLockDurationState.HOLDING) {
                assert lockState != IntrinsicLockDurationState.HOLDING;
                heldLocks.remove(mutableState);
            }
            if (mutableState.getLockState() == IntrinsicLockDurationState.IDLE) {
                assert lockState != IntrinsicLockDurationState.IDLE;
                nonIdleLocks.add(mutableState);
            } else if (lockState == IntrinsicLockDurationState.IDLE) {
                nonIdleLocks.remove(mutableState);
            }
            if (lockState == IntrinsicLockDurationState.HOLDING) {
                heldLocks.add(mutableState);
            }
            mutableState._id = id;
            mutableState._object = object;
            mutableState._trace = trace;
            mutableState._time = time;
            mutableState._startEvent = startEvent;
            mutableState._lockState = lockState;
        }

    }

    private final TLongObjectMap<ThreadState> f_threadToLockToState = new TLongObjectHashMap<ThreadState>();
    private final TLongObjectMap<IntrinsicLockDurationState> f_threadToStatus = new TLongObjectHashMap<IntrinsicLockDurationState>();

    static class Edge extends DefaultEdge {
        private static final long serialVersionUID = 1L;
        final Long lockHeld;
        final Long lockAcquired;
        final TLongSet threads;
        private Timestamp first;
        private Timestamp last;
        private long count;

        Edge(final Long held, final Long acq) {
            lockHeld = held;
            lockAcquired = acq;
            threads = new TLongHashSet(EDGE_HINT);
        }

        void addThread(long thread) {
            threads.add(thread);
        }

        public void setFirst(final Timestamp t) {
            if (first != null) {
                throw new IllegalStateException("Already set first time");
            }
            first = last = t;
            count = 1;
        }

        public void updateLast(final Timestamp time) {
            if (time != null && time.after(last)) {
                last = time;
                count++;
            }
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "Edge [lockHeld=" + lockHeld + ", lockAcquired="
                    + lockAcquired + ", threads=" + threads + ", first="
                    + first + ", last=" + last + ", count=" + count + "]";
        }

    }

    static class EdgeFactory implements org.jgrapht.EdgeFactory<Long, Edge> {
        @Override
        public Edge createEdge(final Long held, final Long acq) {
            return new Edge(held, acq);
        }
    }

    static class RWLock {
        final long id;
        final Long read;
        final Long write;
        final Timestamp time;

        RWLock(final long l, final Long r, final Long w, final Timestamp t) {
            id = l;
            read = r;
            write = w;
            time = t;
        }
    }

    private final TLongObjectMap<TLongObjectMap<Edge>> edgeStorage = new TLongObjectHashMap<TLongObjectMap<Edge>>();

    private static class GraphInfo {
        final TLongSet destinations = new TLongHashSet();

        /**
         * Vertices = locks Edge weight = # of times the edge appears
         */
        final DefaultDirectedGraph<Long, Edge> lockGraph = new DefaultDirectedGraph<Long, Edge>(
                EDGE_FACTORY);
    }

    private boolean flushed = false;

    public IntrinsicLockDurationRowInserter(final Connection c)
            throws SQLException {
        int i = 0;
        for (final String q : queries) {
            statements[i++] = c.prepareStatement(q);
        }
    }

    public void flush(final Timestamp endTime) throws SQLException {
        if (flushed) {
            return;
        }
        flushed = true;

        handleNonIdleFinalState(endTime);

        if (StaticCallLocation.checkSites) {
            final Set<Entry<Long, Boolean>> refd = TraceNode.refdSites
                    .entrySet();
            for (final Entry<Long, Boolean> e : refd) {
                final long id = e.getKey();
                if (!StaticCallLocation.validSites.contains(id)) {
                    log.severe("Couldn't find site " + id);
                }
            }
        }

        // FIX replace the graph w/ own implementation from CLR 23.5
        final GraphInfo info = createGraphFromStorage();
        final CycleDetector<Long, Edge> detector = new CycleDetector<Long, Edge>(
                info.lockGraph);
        if (detector.detectCycles()) {
            final StrongConnectivityInspector<Long, Edge> inspector = new StrongConnectivityInspector<Long, Edge>(
                    info.lockGraph);
            for (final Set<Long> comp : inspector.stronglyConnectedSets()) {
                final List<Edge> graphEdges = new ArrayList<Edge>();
                // Compute the set of edges myself
                // (since the library's inefficient at iterating over edges)
                for (final long src : comp) {
                    final TLongObjectMap<Edge> edges = edgeStorage.get(src);
                    if (edges == null) {
                        // Ignorable because it's (probably) part of a RW lock
                        continue;
                    }
                    // Only look at edges in the component
                    for (final long dest : comp) {
                        final Edge e = edges.get(dest);
                        if (e != null) {
                            graphEdges.add(e);
                            // outputCycleEdge(f_cyclePS, compId, e);
                        }
                    }
                }
                new CycleEnumerator(graphEdges).enumerate();
            }
        }
        for (int i = 0; i < queries.length; i++) {
            if (counts[i] > 0) {
                statements[i].executeBatch();
                counts[i] = 0;
            }
        }
    }

    class CycleEnumerator extends CombinationEnumerator<Edge> {
        final List<Set<Edge>> foundCycles;
        int cycleId;

        CycleEnumerator(List<Edge> edges) {
            super(edges);
            foundCycles = new ArrayList<Set<Edge>>();
        }

        @Override
        void handleEnumeration(Set<Edge> cycle) {
            DirectedGraph<Long, Edge> graph = new DefaultDirectedGraph<Long, Edge>(
                    EDGE_FACTORY);
            for (Set<Edge> found : foundCycles) {
                if (cycle.containsAll(found)) {
                    return;
                }
            }
            for (Edge e : cycle) {
                graph.addVertex(e.lockAcquired);
                graph.addVertex(e.lockHeld);
                graph.addEdge(e.lockHeld, e.lockAcquired, e);
            }
            StrongConnectivityInspector<Long, Edge> i = new StrongConnectivityInspector<Long, Edge>(
                    graph);
            if (i.isStronglyConnected()) {
                cycle = sanitizeGraph(cycle, graph);
                if (!cycle.isEmpty()) {
                    for (Edge e : cycle) {
                        try {
                            outputCycleEdge(statements[LOCK_CYCLE], cycleId, e);
                        } catch (SQLException e1) {
                            throw new IllegalStateException(e1);
                        }
                    }
                }
                cycleId++;
                foundCycles.add(cycle);
            }
        }

        Set<Edge> sanitizeGraph(Set<Edge> cycle, DirectedGraph<Long, Edge> graph) {
            final Set<Edge> deleted = new HashSet<Edge>(cycle.size());
            final TLongSet threads = new TLongHashSet();
            for (Edge e : cycle) {
                if (!deleted.contains(e)) {
                    threads.addAll(e.threads);
                    Edge e_p = graph.outgoingEdgesOf(e.lockAcquired).iterator()
                            .next();
                    if (e_p.threads.equals(e.threads)
                            && e_p.lockAcquired != e.lockHeld) {
                        deleted.add(e);
                        deleted.add(e_p);
                        graph.removeEdge(e);
                        graph.removeEdge(e_p);
                        Edge newEdge = graph.addEdge(e.lockHeld,
                                e_p.lockAcquired);
                        newEdge.threads.addAll(e.threads);
                        newEdge.first = e.first;
                        newEdge.last = e_p.last;
                        newEdge.count = e_p.count;
                        graph.removeVertex(e.lockAcquired);
                    }
                }
            }
            if (threads.size() == 1) {
                Collections.emptySet();
            }
            return graph.edgeSet();
        }
    }

    private void outputCycleEdge(final PreparedStatement f_cyclePS,
            final int compId, final Edge e) throws SQLException {
        // Should only be output once
        int idx = 1;
        f_cyclePS.setInt(idx++, compId);
        f_cyclePS.setLong(idx++, e.lockHeld);
        f_cyclePS.setLong(idx++, e.lockAcquired);
        f_cyclePS.setLong(idx++, e.count);
        f_cyclePS.setTimestamp(idx++, e.first, here);
        f_cyclePS.setTimestamp(idx++, e.last, here);
        if (doInsert) {
            f_cyclePS.addBatch();
            if (++counts[LOCK_CYCLE] == 10000) {
                f_cyclePS.executeBatch();
                counts[LOCK_CYCLE] = 0;
            }
        }
    }

    private static final boolean omitEdges = true;

    private GraphInfo createGraphFromStorage() {

        final GraphInfo info = new GraphInfo();
        // Compute the set of destinations (used for pruning)
        edgeStorage.forEachValue(new TObjectProcedure<TLongObjectMap<Edge>>() {
            @Override
            public boolean execute(TLongObjectMap<Edge> object) {
                object.forEachKey(new TLongProcedure() {
                    @Override
                    public boolean execute(long value) {
                        info.destinations.add(value);
                        return true;
                    }
                });
                return true;
            }
        });
        edgeStorage
                .forEachEntry(new TLongObjectProcedure<TLongObjectMap<Edge>>() {
                    int edges = 0;
                    int omitted = 0;

                    @Override
                    public boolean execute(long src, TLongObjectMap<Edge> map) {

                        // Note: destinations already compensates for some of
                        // these being RW
                        // locks
                        if (omitEdges && !info.destinations.contains(src)) {
                            // The source is a root lock, so we can omit its
                            // edges
                            // (e.g. always the first lock acquired)
                            final int num = map.size();
                            // System.out.println("Omitting "+num+" edges for #"+src);
                            omitted += num;
                            return true;
                        }
                        map.forEachEntry(new TLongObjectProcedure<Edge>() {
                            Long source = null;

                            @Override
                            public boolean execute(long dest, Edge e) {
                                if (omitEdges && edgeStorage.get(dest) == null) {
                                    // The destination is a leaf lock, so we can
                                    // omit it
                                    // (e.g. no more locks are acquired after
                                    // holding it)

                                    // System.out.println("Omitting edge to #"+dest);
                                    omitted++;
                                    return true;
                                }
                                if (source == null) {
                                    source = e.lockHeld;
                                    info.lockGraph.addVertex(source);
                                }
                                info.lockGraph.addVertex(e.lockAcquired);
                                info.lockGraph.addEdge(source, e.lockAcquired,
                                        e);
                                edges++;
                                return true;
                            }
                        });
                        log.info("Total edges = " + edges + ", omitted = "
                                + omitted);
                        return true;
                    }
                });

        return info;
    }

    private void handleNonIdleFinalState(final Timestamp endTime)
            throws SQLException {
        f_threadToStatus
                .forEachEntry(new TLongObjectProcedure<IntrinsicLockDurationState>() {
                    boolean createdEvent = false;

                    @Override
                    public boolean execute(long thread,
                            IntrinsicLockDurationState status) {
                        log.info("Thread " + thread + " : " + status);
                        final ThreadState lockToState = f_threadToLockToState
                                .get(thread);
                        if (lockToState != null) {
                            for (final State state : lockToState.nonIdleLocks()) {
                                log.info("\tLock " + state.getLock() + " : "
                                        + state.getLockState());
                                if (!createdEvent) {
                                    createdEvent = true;
                                    try {
                                        insertLock(true, endTime, thread,
                                                state.getTrace(), /*
                                                                   * src is
                                                                   * nonsense
                                                                   */
                                                state.getLock(),
                                                state.getLockObject(),
                                                LockType.INTRINSIC,
                                                LockState.AFTER_RELEASE, true,
                                                false, false);
                                    } catch (SQLException e) {
                                        throw new IllegalStateException(e);
                                    }
                                }
                                if (state.getLockState() == IntrinsicLockDurationState.BLOCKING) {
                                    noteHeldLocks(FINAL_EVENT, endTime, thread,
                                            state.getLock(), lockToState);
                                }
                                recordStateDuration(thread, state.getLock(),
                                        state.getTime(), state.getId(),
                                        endTime, FINAL_EVENT,
                                        state.getLockState());
                            }
                        }
                        return true;
                    }
                });
    }

    public void close() throws SQLException {
        for (int i = 0; i < statements.length; i++) {
            if (statements[i] != null) {
                statements[i].close();
                statements[i] = null;
            }
        }
        f_threadToLockToState.clear();
        f_threadToStatus.clear();
        // FIX clear lockGraph
    }

    private ThreadState getLockToStateMap(final long inThread) {
        ThreadState lockToState = f_threadToLockToState.get(inThread);
        if (lockToState == null) {
            lockToState = new ThreadState();
            f_threadToLockToState.put(inThread, lockToState);
        }
        return lockToState;
    }

    private void updateState(final State state, final long id,
            final Timestamp time, final long inThread, final long trace,
            final long object, final LockState startEvent,
            final IntrinsicLockDurationState lockState,
            final ThreadState lockToState) {
        final IntrinsicLockDurationState oldLockState = state.getLockState();
        lockToState.update(state, id, trace, object, time, startEvent,
                lockState);
        updateThreadStatus(id, time, inThread, oldLockState, lockState);
    }

    private void updateThreadStatus(final long id, final Timestamp time,
            final long inThread, final IntrinsicLockDurationState oldLockState,
            final IntrinsicLockDurationState newLockState) {

        boolean noneFound = f_threadToStatus
                .forEachEntry(new TLongObjectProcedure<IntrinsicLockDurationState>() {
                    @Override
                    public boolean execute(long thisThread,
                            IntrinsicLockDurationState thisStatus) {
                        // Check if it's the current thread
                        if (inThread == thisThread) {
                            f_threadToStatus.put(
                                    thisThread,
                                    computeThisThreadStatus(thisStatus,
                                            oldLockState, newLockState,
                                            inThread));
                            return false;
                        }
                        return true;
                    }
                });
        if (noneFound) {
            // insert the entry
            final IntrinsicLockDurationState newState = computeThisThreadStatus(
                    IntrinsicLockDurationState.IDLE, oldLockState,
                    newLockState, inThread);
            f_threadToStatus.put(inThread, newState);
        }
    }

    /**
     * Update this thread's status, based on an event on one of the locks
     */
    private IntrinsicLockDurationState computeThisThreadStatus(
            final IntrinsicLockDurationState threadState,
            final IntrinsicLockDurationState oldLockState,
            final IntrinsicLockDurationState newLockState, final long inThread) {
        switch (oldLockState) {
        default:
        case IDLE:
        case HOLDING:
            assert threadState.isRunning();
            return computeRunningThreadStatus(newLockState, inThread);
        case BLOCKING:
            assert threadState == IntrinsicLockDurationState.BLOCKING;
            assert newLockState == IntrinsicLockDurationState.HOLDING;
            return newLockState;
        case WAITING:
            assert threadState == IntrinsicLockDurationState.WAITING;
            assert newLockState == IntrinsicLockDurationState.HOLDING;
            return newLockState;
        }
    }

    private IntrinsicLockDurationState computeRunningThreadStatus(
            final IntrinsicLockDurationState newLockState, final long inThread) {
        if (newLockState == IntrinsicLockDurationState.IDLE) {
            // Need to check the status on the other locks
            final ThreadState lockToState = getLockToStateMap(inThread);
            for (final State state : lockToState.nonIdleLocks()) {
                assert state.getLockState().isRunning();
                return state.getLockState();
            }
            // Otherwise idle
        }
        return newLockState;
    }

    public void event(final long id, final Timestamp time, final long inThread,
            final long trace, final long lock, final long object,
            final LockState lockEvent, final boolean success) {
        // A failed release attempt changes no states.
        if (lockEvent == LockState.AFTER_RELEASE && !success) {
            return;
        }
        final ThreadState lockToState = getLockToStateMap(inThread);
        final State state = lockToState.getState(lock);
        switch (state.getLockState()) {
        case IDLE:
            // Only legal transition is BA->Block
            assert state.getTimesEntered() == 0;
            if (lockEvent == LockState.BEFORE_ACQUISITION) {
                // No need to record idle time
                updateState(state, id, time, inThread, trace, object,
                        lockEvent, IntrinsicLockDurationState.BLOCKING,
                        lockToState);
            } else {
                logBadEventTransition(inThread, lock, lockEvent, state);
            }
            break;
        case BLOCKING:
            handlePossibleLockAcquire(id, time, inThread, trace, lock, object,
                    lockEvent, LockState.AFTER_ACQUISITION, lockToState, state,
                    success);
            break;
        case HOLDING:
            handleEventWhileHolding(id, time, inThread, trace, lock, object,
                    lockEvent, state, lockToState);
            break;
        case WAITING:
            handlePossibleLockAcquire(id, time, inThread, trace, lock, object,
                    lockEvent, LockState.AFTER_WAIT, lockToState, state, true);
            break;
        default:
            SLLogger.getLogger().log(Level.SEVERE,
                    I18N.err(103, state.getLockState().toString()));
        }
    }

    /**
     * Legal transitions: BA: Hold AA: Hold+1 BW: Wait-1 AR: Idle-1 if
     * timesEntered = 1, otherwise Hold-1
     */
    private void handleEventWhileHolding(final long id, final Timestamp time,
            final long inThread, final long trace, final long lock,
            final long object, final LockState lockEvent, final State state,
            final ThreadState lockToState) {
        assert state.getTimesEntered() > 0;
        switch (lockEvent) {
        case BEFORE_ACQUISITION:
            // Nothing to do right now
            break;
        case AFTER_ACQUISITION:
            state.lockAcquired();
            break;
        case BEFORE_WAIT:
            state.lockReleased();
            recordStateDuration(inThread, lock, state.getTime(), state.getId(),
                    time, id, state.getLockState());
            updateState(state, id, time, inThread, trace, object, lockEvent,
                    IntrinsicLockDurationState.WAITING, lockToState);
            break;
        case AFTER_WAIT:
            logBadEventTransition(inThread, lock, lockEvent, state);
            break;
        case AFTER_RELEASE:
            state.lockReleased();

            final IntrinsicLockDurationState newState = state.getTimesEntered() == 0 ? IntrinsicLockDurationState.IDLE
                    : IntrinsicLockDurationState.HOLDING;
            if (state.getTimesEntered() == 0) {
                recordStateDuration(inThread, lock, state.getTime(),
                        state.getId(), time, id, state.getLockState());
                updateState(state, id, time, inThread, trace, object,
                        lockEvent, newState, lockToState);
            }
            break;
        default:
            SLLogger.getLogger().log(Level.SEVERE,
                    I18N.err(104, lockEvent.toString()));
        }
    }

    private void handlePossibleLockAcquire(final long id, final Timestamp time,
            final long inThread, final long trace, final long lock,
            final long object, final LockState lockEvent,
            final LockState eventToMatch, final ThreadState lockToState,
            final State state, final boolean success) {
        if (lockEvent == eventToMatch) {
            assert state.getTimesEntered() >= 0;
            if (success) {
                noteHeldLocks(id, time, inThread, lock, lockToState);
            }
            recordStateDuration(inThread, lock, state.getTime(), state.getId(),
                    time, id, state.getLockState());
            updateState(state, id, time, inThread, trace, object, lockEvent,
                    success ? IntrinsicLockDurationState.HOLDING
                            : IntrinsicLockDurationState.IDLE, lockToState);
            state.lockAcquired();
        } else {
            logBadEventTransition(inThread, lock, lockEvent, state);
        }
    }

    private void logBadEventTransition(final long inThread, final long lock,
            final LockState lockEvent, final State state) {
        SLLogger.getLogger().log(
                Level.SEVERE,
                I18N.err(102, state.getLockState().toString(),
                        lockEvent.toString(), lock, inThread));
    }

    private void noteHeldLocks(final long id, final Timestamp time,
            final long thread, final long lock, final ThreadState lockToState) {
        // Note what other locks are held at the time of this event
        List<State> heldLocks = lockToState.heldLocks();
        if (!heldLocks.isEmpty()) {
            insertLockEdge(time, heldLocks.get(heldLocks.size() - 1).getLock(),
                    lock, thread);
            for (final State state : heldLocks) {
                if (lock == state.getLock()) {
                    continue; // Skip myself
                }
                insertHeldLock(id, time, state.getId(), state.getLock(), lock,
                        thread);
            }
        }
    }

    private void recordStateDuration(final long inThread, final long lock,
            final Timestamp startTime, final long startEvent,
            final Timestamp stopTime, final long stopEvent,
            final IntrinsicLockDurationState state) {
        final PreparedStatement f_ps = statements[LOCK_DURATION];
        try {
            int idx = 1;
            f_ps.setLong(idx++, inThread);
            f_ps.setLong(idx++, lock);
            f_ps.setTimestamp(idx++, startTime, here);
            f_ps.setLong(idx++, startEvent);
            f_ps.setTimestamp(idx++, stopTime, here);
            f_ps.setLong(idx++, stopEvent);

            final long secs = stopTime.getTime() / 1000 - startTime.getTime()
                    / 1000;
            final long nanos = stopTime.getNanos() - startTime.getNanos();

            f_ps.setLong(idx++, 1000000000 * secs + nanos);
            f_ps.setString(idx++, state.toString());
            if (doInsert) {
                f_ps.addBatch();
                if (++counts[LOCK_DURATION] == 10000) {
                    f_ps.executeBatch();
                    counts[LOCK_DURATION] = 0;
                }
            }
        } catch (final SQLException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Insert failed: ILOCKDURATION", e);
        }
    }

    private void insertLockEdge(final Timestamp time, final long lockHeld,
            final long acquired, final long thread) {
        final Long acq = acquired;
        TLongObjectMap<Edge> edges = edgeStorage.get(lockHeld);
        if (edges == null) {
            edges = new TLongObjectHashMap<Edge>(EDGE_HINT);
            edgeStorage.put(lockHeld, edges);
        }
        Edge e = edges.get(acquired);
        if (e == null) {
            e = new Edge(lockHeld, acq);
            e.setFirst(time);
            edges.put(acq, e);
        } else {
            e.updateLast(time);
        }
        e.addThread(thread);

    }

    private void insertHeldLock(final long eventId, final Timestamp time,
            final long lockHeldEventId, final long lockHeld,
            final long acquired, final long thread) {
        final PreparedStatement f_heldLockPS = statements[LOCKS_HELD];
        try {
            int idx = 1;
            f_heldLockPS.setLong(idx++, eventId);
            f_heldLockPS.setLong(idx++, lockHeldEventId);
            f_heldLockPS.setLong(idx++, lockHeld);
            f_heldLockPS.setLong(idx++, acquired);
            f_heldLockPS.setLong(idx++, thread);
            if (doInsert) {
                f_heldLockPS.addBatch();
                if (++counts[LOCKS_HELD] == 10000) {
                    f_heldLockPS.executeBatch();
                    counts[LOCKS_HELD] = 0;
                }
            }
        } catch (final SQLException e) {
            SLLogger.getLogger().log(Level.SEVERE, "Insert failed: ILOCKSHELD",
                    e);
        }
    }

    /**
     * Clear out state for the Object
     */
    public void gcObject(final long id) {
        final Long key = id;
        // Clean up if it's a thread
        f_threadToStatus.remove(key);
        f_threadToLockToState.remove(key);
        f_threadToLockToState.forEachValue(new TObjectProcedure<ThreadState>() {

            @Override
            public boolean execute(ThreadState object) {
                object.gcLock(key);
                return true;
            }

        });

        // TODO Clean up lock graph?
        // Q: how do I know if an object is a lock?

        // Remove references to a lock if
        // 1. It's a "leaf" lock
        // (e.g. no more locks are acquired after holding it)
        // 2. It's a "root" lock
        // (e.g. always the first lock acquired)
    }

    private long f_lockId = 0;

    // Only called here and from Lock
    long insertLock(final boolean finalEvent, final Timestamp time,
            final long inThread, final long trace, final long lock,
            final long object, final LockType lockType,
            final LockState lockState, final Boolean success,
            final Boolean lockIsThis, final Boolean lockIsClass)
            throws SQLException {
        final PreparedStatement ps = statements[INSERT_LOCK];
        int idx = 1;
        ps.setLong(idx++, finalEvent ? Lock.FINAL_EVENT : ++f_lockId);
        ps.setTimestamp(idx++, time, here);
        ps.setLong(idx++, inThread);
        ps.setLong(idx++, trace);
        ps.setLong(idx++, lock); // The aggregate
        ps.setLong(idx++, object); // The actual object locked on
        ps.setString(idx++, lockType.getFlag());
        ps.setString(idx++, lockState.toString().replace('_', ' '));
        JDBCUtils.setNullableBoolean(idx++, ps, success);
        JDBCUtils.setNullableBoolean(idx++, ps, lockIsThis);
        JDBCUtils.setNullableBoolean(idx++, ps, lockIsClass);
        if (doInsert) {
            ps.addBatch();
            if (++counts[INSERT_LOCK] == 10000) {
                ps.executeBatch();
                counts[INSERT_LOCK] = 0;
            }
        }
        return f_lockId;
    }
}
