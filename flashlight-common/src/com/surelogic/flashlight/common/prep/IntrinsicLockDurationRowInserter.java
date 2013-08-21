package com.surelogic.flashlight.common.prep;

import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.INSERT_LOCK;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCKS_HELD;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_COMPONENT;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_CYCLE;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_DURATION;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_TRACE;
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
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

    private static final boolean doInsert = AbstractPrep.doInsert;

    enum Queries {
        LOCK_DURATION(
                "INSERT INTO LOCKDURATION (InThread,Lock,Start,StartEvent,Stop,StopEvent,Duration,State) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"), LOCKS_HELD(
                "INSERT INTO LOCKSHELD (LockEvent,LockHeldEvent,LockHeld,LockAcquired,InThread) VALUES (?, ?, ?, ?, ?)"), LOCK_CYCLE(
                "INSERT INTO LOCKCYCLE (Component,LockHeld,LockAcquired,Count,FirstTime,LastTime) VALUES (?, ?, ?, ?, ?, ?)"), INSERT_LOCK(
                "INSERT INTO LOCK (Id,TS,InThread,Trace,LockTrace,Lock,Object,Type,State,Success,LockIsThis,LockIsClass) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"), LOCK_COMPONENT(
                "INSERT INTO LOCKCOMPONENT (Component,Lock) VALUES (?, ?)"), LOCK_TRACE(
                "INSERT INTO LOCKTRACE (Id,Lock,Trace,Parent) VALUES(?,?,?,?)");
        private final String sql;

        Queries(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }

    }

    private final EnumMap<Queries, PreparedStatement> statements = new EnumMap<IntrinsicLockDurationRowInserter.Queries, PreparedStatement>(
            Queries.class);
    private final EnumMap<Queries, Integer> counts = new EnumMap<IntrinsicLockDurationRowInserter.Queries, Integer>(
            Queries.class);
    private final TLongObjectHashMap<List<LockTrace>> lockTraces = new TLongObjectHashMap<List<LockTrace>>();
    private final TLongObjectMap<TLongObjectMap<LockTrace>> lockTraceRoots = new TLongObjectHashMap<TLongObjectMap<LockTrace>>();
    long lockTraceId;
    int cycleId;
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
        LockTrace lockTrace = null;
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
                    + lockAcquired + ", threads=" + threads + "]";
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
        for (Queries q : Queries.values()) {
            statements.put(q, c.prepareStatement(q.getSql()));
            counts.put(q, 0);
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
        final GraphInfo info = createGraphFromStorage();
        computeGraphComponents(info);
        detectLockCycles(info);
        for (Queries q : Queries.values()) {
            if (counts.get(q) > 0) {
                statements.get(q).executeBatch();
                counts.put(q, 0);
            }
        }
    }

    /**
     * Increment count, return true if batch should be executed.
     * 
     * @param q
     * @return
     */
    private boolean incrementCount(Queries q) {
        int count = counts.get(q);
        if (count == 9999) {
            counts.put(q, 0);
            return true;
        } else {
            counts.put(q, count + 1);
            return false;
        }
    }

    private void computeGraphComponents(GraphInfo info) throws SQLException {
        final ConnectivityInspector<Long, Edge> inspector = new ConnectivityInspector<Long, IntrinsicLockDurationRowInserter.Edge>(
                info.lockGraph);
        final PreparedStatement ps = statements.get(LOCK_COMPONENT);
        int i = 0;
        for (Set<Long> set : inspector.connectedSets()) {
            for (long lock : set) {
                ps.setInt(1, i);
                ps.setLong(2, lock);
                if (doInsert) {
                    ps.addBatch();
                    if (incrementCount(LOCK_COMPONENT)) {
                        ps.executeBatch();
                    }
                }
            }
            i++;
        }
    }

    /**
     * Overview:
     * <ol>
     * <li>Generate a graph of lock edges. An edge exists each time a lock is
     * acquired while another is held in the program.
     * <li>Break up the graph into strongly connected components.
     * <li>Construct an enumeration of all the simple cycles in the strongly
     * connected components, from smallest to largest, and check each one for
     * deadlock.
     * 
     * @throws SQLException
     */
    private void detectLockCycles(GraphInfo info) throws SQLException {
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
    }

    class CycleEnumerator extends CombinationEnumerator<Edge> {
        final Set<Set<Edge>> foundCycles;

        CycleEnumerator(List<Edge> edges) {
            super(edges);
            foundCycles = new HashSet<Set<Edge>>();
        }

        /**
         * Cycles will appear in order of increasing number of edges. We check
         * each new edge set against the set of discovered cycles, which
         * prevents us from having any non-simple cycles.
         */
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
                foundCycles.add(cycle);
                Set<Edge> sanitizedCycle = sanitizeGraph(cycle, graph);
                if (sanitizedCycle.size() > 1
                        && (sanitizedCycle.equals(cycle) || foundCycles
                                .add(sanitizedCycle))) {
                    // This is a cycle whose ideal form we haven't written out
                    // yet, so let's do it. I'm also pretty sure that I don't
                    // need the foundCycles check because any sanitized cycle
                    // will be smaller than the current one and therefore
                    // already considered, but I'm including it anyways
                    // for good measure.
                    if (isDeadlock(sanitizedCycle, graph)) {
                        for (Edge e : cycle) {
                            try {
                                outputCycleEdge(statements.get(LOCK_CYCLE),
                                        cycleId, e);
                            } catch (SQLException e1) {
                                throw new IllegalStateException(e1);
                            }
                        }
                        cycleId++;
                    }
                }
            }
        }

        private boolean isDeadlock(Set<Edge> cycle,
                final DirectedGraph<Long, Edge> graph) {
            final Edge start = cycle.iterator().next();
            final Visited nodes = new Visited(start.lockAcquired);
            return !start.threads.forEach(new TLongProcedure() {

                @Override
                public boolean execute(long thread) {
                    Visited threads = new Visited(thread);
                    // Try walking back to the start using each thread
                    return !deadlockHelper(start, threads, nodes, graph,
                            start.lockHeld);
                }
            });
        }

        boolean deadlockHelper(Edge current, final Visited threads,
                final Visited nodes, final DirectedGraph<Long, Edge> graph,
                final long firstNode) {
            if (current.lockAcquired == firstNode) {
                // We are done, this is a full cycle
                return true;
            }
            final Set<Edge> edges = graph.outgoingEdgesOf(current.lockAcquired);
            for (final Edge nextEdge : edges) {
                // Check to see if the node is on the visited list, otherwise
                // check to see if we can find our deadlock using any of the
                // threads along this edge
                if (nodes.contains(nextEdge.lockAcquired)) {
                    continue;
                }
                if (!nextEdge.threads.forEach(new TLongProcedure() {

                    @Override
                    public boolean execute(long thread) {
                        // We only consider threads that we haven't seen before.
                        if (threads.contains(thread)) {
                            return true;
                        }
                        // If we are back at the start then we are done
                        if (nextEdge.lockAcquired == firstNode) {
                            return false;
                        }
                        // Otherwise as long as we haven't been there we should
                        // consider it
                        return !deadlockHelper(nextEdge, new Visited(thread,
                                threads), new Visited(nextEdge.lockAcquired,
                                nodes), graph, firstNode);
                    }
                })) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Many cycles can be improved getting rid of intermediate nodes that
         * aren't strictly necessary, or ruled out completely when they don't
         * actually deadlock in practice based on the threads observed. This
         * routine replaces adjacent edges that are accessed by the same set of
         * threads with their closure. It also returns an empty set of the cycle
         * consists of only one thread.
         * 
         * @param cycle
         * @param graph
         * @return
         */
        private Set<Edge> sanitizeGraph(Set<Edge> cycle,
                DirectedGraph<Long, Edge> graph) {
            final Set<Edge> deleted = new HashSet<Edge>(cycle.size());
            final TLongSet threads = new TLongHashSet();
            for (Edge e : cycle) {
                if (!deleted.contains(e)) {
                    threads.addAll(e.threads);
                    Edge e_p = graph.outgoingEdgesOf(e.lockAcquired).iterator()
                            .next();
                    if (e_p.threads.equals(e.threads)
                            && e_p.lockAcquired != e.lockHeld) {
                        TLongObjectMap<Edge> heldMap = edgeStorage
                                .get(e.lockHeld);
                        if (heldMap != null) {
                            Edge edge = heldMap.get(e_p.lockAcquired);
                            if (edge != null) {
                                deleted.add(e);
                                deleted.add(e_p);
                                graph.removeEdge(e);
                                graph.removeEdge(e_p);
                                graph.addEdge(e.lockHeld, e_p.lockAcquired,
                                        edge);
                                graph.removeVertex(e.lockAcquired);
                            }
                        }
                    }
                }
            }
            if (threads.size() == 1) {
                return Collections.emptySet();
            }
            return graph.edgeSet();
        }
    }

    static class Visited {
        final long first;
        final Visited rest;

        Visited() {
            first = -1;
            rest = null;
        }

        Visited(long elem) {
            first = elem;
            rest = new Visited();
        }

        Visited(long first, Visited rest) {
            this.first = first;
            // We don't want duplicate entries to stack b/c it breaks what we
            // are using restContains to find out
            if (this.first == rest.first) {
                this.rest = rest.rest;
            } else {
                this.rest = rest;
            }
        }

        boolean restContains(long elem) {
            return rest != null && rest.contains(elem);
        }

        boolean contains(long elem) {
            return first == elem || rest != null && rest.contains(elem);
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
            if (incrementCount(LOCK_CYCLE)) {
                f_cyclePS.executeBatch();
            }
        }
    }

    private static final boolean omitEdges = false;

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
                        log.finest("Total edges = " + edges + ", omitted = "
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
                        log.finest("Thread " + thread + " : " + status);
                        final ThreadState lockToState = f_threadToLockToState
                                .get(thread);
                        if (lockToState != null) {
                            for (final State state : lockToState.nonIdleLocks()) {
                                log.finest("\tLock " + state.getLock() + " : "
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
        for (PreparedStatement ps : statements.values()) {
            ps.close();
        }
        statements.clear();
        counts.clear();
        f_threadToLockToState.clear();
        f_threadToStatus.clear();
        lockTraces.clear();
        edgeStorage.clear();
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
        for (final State state : heldLocks) {
            if (lock == state.getLock()) {
                continue; // Skip myself
            }
            insertHeldLock(id, time, state.getId(), state.getLock(), lock,
                    thread);
        }
    }

    private void recordStateDuration(final long inThread, final long lock,
            final Timestamp startTime, final long startEvent,
            final Timestamp stopTime, final long stopEvent,
            final IntrinsicLockDurationState state) {
        final PreparedStatement f_ps = statements.get(LOCK_DURATION);
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
                if (incrementCount(LOCK_DURATION)) {
                    f_ps.executeBatch();
                }
            }
        } catch (final SQLException e) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Insert failed: ILOCKDURATION", e);
        }
    }

    /**
     * Inserts a lock into our lock graph.
     * 
     * @param time
     * @param lockHeld
     * @param acquired
     * @param thread
     */
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
        final PreparedStatement f_heldLockPS = statements.get(LOCKS_HELD);
        try {
            int idx = 1;
            f_heldLockPS.setLong(idx++, eventId);
            f_heldLockPS.setLong(idx++, lockHeldEventId);
            f_heldLockPS.setLong(idx++, lockHeld);
            f_heldLockPS.setLong(idx++, acquired);
            f_heldLockPS.setLong(idx++, thread);
            if (doInsert) {
                f_heldLockPS.addBatch();
                if (incrementCount(LOCKS_HELD)) {
                    f_heldLockPS.executeBatch();
                }
            }
        } catch (final SQLException e) {
            SLLogger.getLogger().log(Level.SEVERE, "Insert failed: ILOCKSHELD",
                    e);
        }
        /*
         * Create an edge in our lock graph every time a lock is acquired where
         * a lock is held.
         */
        insertLockEdge(time, lockHeld, acquired, thread);
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

        gcLock(id);

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
        final ThreadState threadState = getLockToStateMap(inThread);
        switch (lockState) {
        case AFTER_ACQUISITION:
            if (success != Boolean.FALSE) {
                threadState.lockTrace = pushLockTrace(threadState.lockTrace,
                        lock, trace);
            }
            break;
        case AFTER_RELEASE:
            if (success != Boolean.FALSE) {
                threadState.lockTrace = popLockTrace(threadState.lockTrace,
                        lock);
            }
            break;
        default:
            // Do nothing
        }
        final PreparedStatement ps = statements.get(INSERT_LOCK);
        int idx = 1;
        ps.setLong(idx++, finalEvent ? Lock.FINAL_EVENT : ++f_lockId);
        ps.setTimestamp(idx++, time, here);
        ps.setLong(idx++, inThread);
        ps.setLong(idx++, trace);
        if (threadState.lockTrace == null) {
            ps.setNull(idx++, Types.BIGINT);
        } else {
            ps.setLong(idx++, threadState.lockTrace.getId());
        }
        ps.setLong(idx++, lock); // The aggregate
        ps.setLong(idx++, object); // The actual object locked on
        ps.setString(idx++, lockType.getFlag());
        ps.setString(idx++, lockState.toString().replace('_', ' '));
        JDBCUtils.setNullableBoolean(idx++, ps, success);
        JDBCUtils.setNullableBoolean(idx++, ps, lockIsThis);
        JDBCUtils.setNullableBoolean(idx++, ps, lockIsClass);
        if (doInsert) {
            ps.addBatch();
            if (incrementCount(INSERT_LOCK)) {
                ps.executeBatch();
            }
        }
        return f_lockId;
    }

    /**
     * Pop a lock off the current lock trace. The lock does not need to be the
     * topmost lock.
     * 
     * @param current
     * @param lock
     * @return
     * @throws SQLException
     */
    private LockTrace popLockTrace(LockTrace current, long lock)
            throws SQLException {
        if (current.getLock() == lock) {
            return current.getParent();
        } else {
            return pushLockTrace(popLockTrace(current.getParent(), lock),
                    current.getLock(), current.getTrace());
        }
    }

    /**
     * Push a new lock onto the current lock trace, and return that lock trace.
     * 
     * @param current
     * @param lock
     * @return
     * @throws SQLException
     */
    private LockTrace pushLockTrace(LockTrace current, long lock, long trace)
            throws SQLException {
        LockTrace lockTrace;
        if (current == null) {
            // Try to get the root lock trace if it exists, otherwise make it.
            TLongObjectMap<LockTrace> traceRoots = lockTraceRoots.get(lock);
            if (traceRoots == null) {
                traceRoots = new TLongObjectHashMap<LockTrace>();
                lockTraceRoots.put(lock, traceRoots);
            }
            lockTrace = traceRoots.get(trace);
            if (lockTrace != null) {
                return lockTrace;
            }
            lockTrace = LockTrace.newRootLockTrace(lockTraceId++, lock, trace);
            traceRoots.put(trace, lockTrace);
        } else {
            // Use the cached version if we've got it
            for (LockTrace child : current.children()) {
                if (child.matches(lock, trace)) {
                    return child;
                }
            }
            lockTrace = current.pushLockTrace(lockTraceId++, lock, trace);
        }
        // Insert a new lock. We should have already returned from the method
        // if one had been found already.
        PreparedStatement st = statements.get(LOCK_TRACE);
        // INSERT INTO LOCKTRACE (Id,Lock,Trace,Parent) VALUES(?,?,?,?)
        int idx = 1;
        st.setLong(idx++, lockTrace.getId());
        st.setLong(idx++, lock);
        st.setLong(idx++, trace);
        if (lockTrace.getParent() == null) {
            // We make the first locks acquired self-referential
            st.setLong(idx++, lockTrace.getId());
        } else {
            st.setLong(idx++, lockTrace.getParent().getId());
        }
        st.addBatch();
        if (incrementCount(LOCK_TRACE)) {
            st.executeBatch();
        }
        List<LockTrace> list = lockTraces.get(lock);
        if (list == null) {
            list = new ArrayList<LockTrace>();
            lockTraces.put(lock, list);
        }
        list.add(lockTrace);
        return lockTrace;
    }

    /**
     * Remove all references to a garbage collected lock
     * 
     * @param lock
     */
    private void gcLock(long lock) {
        for (LockTrace lockTrace : lockTraces.put(lock, null)) {
            lockTrace.expunge();
        }
        lockTraceRoots.put(lock, null);
    }
}
