package com.surelogic.flashlight.common.prep;

import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.INSERT_LOCK;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCKS_HELD;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_CYCLE;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_DURATION;
import static com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter.Queries.LOCK_TRACE;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TObjectProcedure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.JDBCUtils;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.DeadlockAnalyzer;
import com.surelogic.flashlight.common.DeadlockAnalyzer.CycleHandler;
import com.surelogic.flashlight.common.DeadlockAnalyzer.DeadlockAnalysis;
import com.surelogic.flashlight.common.DeadlockAnalyzer.Edge;
import com.surelogic.flashlight.common.LockId;
import com.surelogic.flashlight.common.LockType;

public final class IntrinsicLockDurationRowInserter {

    private static final long FINAL_EVENT = Lock.FINAL_EVENT;

    private static final boolean doInsert = AbstractPrep.doInsert;

    enum Queries {
        LOCK_DURATION(
                "INSERT INTO LOCKDURATION (InThread,Lock,Type,Start,StartEvent,StartTrace,Stop,StopEvent,StopTrace,Duration,State) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)"), LOCKS_HELD(
                        "INSERT INTO LOCKSHELD (LockEvent,LockHeldEvent,LockHeld,LockHeldType,LockAcquired,LockAcquiredType,InThread) VALUES (?, ?, ?, ?, ?, ?, ?)"), LOCK_CYCLE(
                                "INSERT INTO LOCKCYCLE (Component,LockHeld,LockHeldType,LockAcquired,LockAcquiredType,Count,FirstTime,LastTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"), INSERT_LOCK(
                                        "INSERT INTO LOCK (Id,TS,InThread,Trace,LockTrace,Lock,Object,Type,State,Success,LockIsThis) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"), LOCK_TRACE(
                                                "INSERT INTO LOCKTRACE (Id,Lock,Type,Trace,Parent) VALUES(?,?,?,?,?)");
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
    private final Map<LockId, List<LockTrace>> lockTraces = new HashMap<LockId, List<LockTrace>>();
    private final Map<LockId, TLongObjectMap<LockTrace>> lockTraceRoots = new HashMap<LockId, TLongObjectMap<LockTrace>>();

    long lockTraceIdSeq;
    int cycleId;
    private final Calendar here = new GregorianCalendar();
    private final Logger log = SLLogger
            .getLoggerFor(IntrinsicLockDurationRowInserter.class);

    static class State {
        IntrinsicLockDurationState _lockState = IntrinsicLockDurationState.IDLE;
        int _timesEntered = 0;

        // Info about the event that started us in this state
        long _id;
        LockId _lock;
        long _object;
        long _trace;
        LockTrace _lockTrace;
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

        public LockId getLock() {
            return _lock;
        }

        public long getTrace() {
            return _trace;
        }

        public LockTrace getLockTrace() {
            return _lockTrace;
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
        Map<LockId, State> lockToState = new HashMap<LockId, State>();
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
        public void gcLock(final long key) {
            LockId util = new LockId(key, LockType.UTIL);
            LockId intr = new LockId(key, LockType.INTRINSIC);
            State state = lockToState.remove(util);
            if (state != null) {
                nonIdleLocks.remove(state);
                heldLocks.remove(state);
            }
            state = lockToState.remove(intr);
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
        public State getState(final LockId lock) {
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
                final long trace, final LockTrace lockTrace, final long object,
                final Timestamp time, final LockState startEvent,
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
            mutableState._lockTrace = lockTrace;
        }

    }

    private final TLongObjectMap<ThreadState> f_threadToLockToState = new TLongObjectHashMap<ThreadState>();
    private final TLongObjectMap<IntrinsicLockDurationState> f_threadToStatus = new TLongObjectHashMap<IntrinsicLockDurationState>();

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

    private final DeadlockAnalyzer analyzer = new DeadlockAnalyzer();

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
        DeadlockAnalysis analysis = analyzer.beginAnalysis();
        analysis.detectLockCycles(new CycleHandler() {
            int cycleId;

            @Override
            public void cycle(Set<Edge> cycle) {
                for (Edge edge : cycle) {
                    try {
                        outputCycleEdge(statements.get(LOCK_CYCLE), cycleId,
                                edge);
                    } catch (SQLException exc) {
                        throw new IllegalStateException(exc);
                    }
                }
                cycleId++;
            }

        });
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

    static class Visited<T> {
        final T first;
        final Visited<T> rest;

        Visited() {
            first = null;
            rest = null;
        }

        Visited(T elem) {
            first = elem;
            rest = new Visited<T>();
        }

        Visited(T first, Visited<T> rest) {
            this.first = first;
            // We don't want duplicate entries to stack b/c it breaks what we
            // are using restContains to find out
            if (this.first.equals(rest.first)) {
                this.rest = rest.rest;
            } else {
                this.rest = rest;
            }
        }

        boolean restContains(T elem) {
            return rest != null && rest.contains(elem);
        }

        boolean contains(T elem) {
            return elem.equals(first) || rest != null && rest.contains(elem);
        }
    }

    private void outputCycleEdge(final PreparedStatement f_cyclePS,
            final int compId, final Edge e) throws SQLException {
        // Should only be output once
        int idx = 1;
        f_cyclePS.setInt(idx++, compId);
        f_cyclePS.setLong(idx++, e.getLockHeld().getId());
        f_cyclePS.setString(idx++, e.getLockHeld().getType().getFlag());
        f_cyclePS.setLong(idx++, e.getLockAcquired().getId());
        f_cyclePS.setString(idx++, e.getLockAcquired().getType().getFlag());
        f_cyclePS.setLong(idx++, e.getCount());
        f_cyclePS.setTimestamp(idx++, e.getFirst(), here);
        f_cyclePS.setTimestamp(idx++, e.getLast(), here);
        if (doInsert) {
            f_cyclePS.addBatch();
            if (incrementCount(LOCK_CYCLE)) {
                f_cyclePS.executeBatch();
            }
        }
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
                                        LockState.AFTER_RELEASE, true,
                                        false);
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
                                state.getLockTrace(), endTime,
                                FINAL_EVENT, lockToState.lockTrace,
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
        analyzer.clear();
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
        lockToState.update(state, id, trace, lockToState.lockTrace, object,
                time, startEvent, lockState);
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
            final long trace, final LockId lock, final long object,
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
            handlePossibleLockAcquire(id, time, inThread, trace,
                    lockToState.lockTrace, lock, object, lockEvent,
                    LockState.AFTER_ACQUISITION, lockToState, state, success);
            break;

        case HOLDING:
            handleEventWhileHolding(id, time, inThread, trace,
                    lockToState.lockTrace, lock, object, lockEvent, state,
                    lockToState);
            break;
        case WAITING:
            handlePossibleLockAcquire(id, time, inThread, trace,
                    lockToState.lockTrace, lock, object, lockEvent,
                    LockState.AFTER_WAIT, lockToState, state, true);
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
            final long inThread, final long trace, final LockTrace lockTrace,
            final LockId lock, final long object, final LockState lockEvent,
            final State state, final ThreadState lockToState) {
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
                    state.getLockTrace(), time, id, lockTrace,
                    state.getLockState());
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
                        state.getId(), state.getLockTrace(), time, id,
                        lockTrace, state.getLockState());
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
            final long inThread, final long trace, final LockTrace lockTrace,
            final LockId lock, final long object, final LockState lockEvent,
            final LockState eventToMatch, final ThreadState lockToState,
            final State state, final boolean success) {
        if (lockEvent == eventToMatch) {
            assert state.getTimesEntered() >= 0;
            if (success) {
                noteHeldLocks(id, time, inThread, lock, lockToState);
            }
            recordStateDuration(inThread, lock, state.getTime(), state.getId(),
                    state.getLockTrace(), time, id, lockTrace,
                    state.getLockState());
            updateState(state, id, time, inThread, trace, object, lockEvent,
                    success ? IntrinsicLockDurationState.HOLDING
                            : IntrinsicLockDurationState.IDLE, lockToState);
            state.lockAcquired();
        } else {
            logBadEventTransition(inThread, lock, lockEvent, state);
        }
    }

    private void logBadEventTransition(final long inThread, final LockId lock,
            final LockState lockEvent, final State state) {
        SLLogger.getLogger().log(
                Level.SEVERE,
                I18N.err(102, state.getLockState().toString(),
                        lockEvent.toString(), lock.getId(), inThread));
    }

    private void noteHeldLocks(final long id, final Timestamp time,
            final long thread, final LockId lock, final ThreadState lockToState) {
        // Note what other locks are held at the time of this event
        List<State> heldLocks = lockToState.heldLocks();
        for (final State state : heldLocks) {
            if (lock.equals(state.getLock())) {
                continue; // Skip myself
            }
            insertHeldLock(id, time, state.getId(), state.getLock(), lock,
                    thread);
        }
    }

    private void recordStateDuration(final long inThread, final LockId lock,
            final Timestamp startTime, final long startEvent,
            final LockTrace startTrace, final Timestamp stopTime,
            final long stopEvent, final LockTrace stopTrace,
            final IntrinsicLockDurationState state) {
        final PreparedStatement f_ps = statements.get(LOCK_DURATION);
        try {
            int idx = 1;
            f_ps.setLong(idx++, inThread);
            f_ps.setLong(idx++, lock.getId());
            f_ps.setString(idx++, lock.getType().getFlag());
            f_ps.setTimestamp(idx++, startTime, here);
            f_ps.setLong(idx++, startEvent);
            if (startTrace == null) {
                f_ps.setNull(idx++, Types.BIGINT);
            } else {
                f_ps.setLong(idx++, startTrace.getId());
            }
            f_ps.setTimestamp(idx++, stopTime, here);
            f_ps.setLong(idx++, stopEvent);
            if (stopTrace == null) {
                f_ps.setNull(idx++, Types.BIGINT);
            } else {
                f_ps.setLong(idx++, stopTrace.getId());
            }
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

    private void insertHeldLock(final long eventId, final Timestamp time,
            final long lockHeldEventId, final LockId lockHeld,
            final LockId acquired, final long thread) {
        final PreparedStatement f_heldLockPS = statements.get(LOCKS_HELD);
        try {
            int idx = 1;
            f_heldLockPS.setLong(idx++, eventId);
            f_heldLockPS.setLong(idx++, lockHeldEventId);
            f_heldLockPS.setLong(idx++, lockHeld.getId());
            f_heldLockPS.setString(idx++, lockHeld.getType().getFlag());
            f_heldLockPS.setLong(idx++, acquired.getId());
            f_heldLockPS.setString(idx++, acquired.getType().getFlag());
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
        analyzer.addEdge(lockHeld, acquired, time, thread);
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
            final long inThread, final long trace, final LockId lock,
            final long object, final LockState lockState,
            final Boolean success, final boolean lockIsThis)
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
            if (success != Boolean.FALSE && !finalEvent) {
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
        ps.setLong(idx++, lock.getId()); // The aggregate
        ps.setLong(idx++, object); // The actual object locked on
        ps.setString(idx++, lock.getType().getFlag());
        ps.setString(idx++, lockState.toString().replace('_', ' '));
        JDBCUtils.setNullableBoolean(idx++, ps, success);
        if (lockIsThis) {
            ps.setString(idx++, "Y");
        } else {
            ps.setNull(idx++, Types.VARCHAR);
        }
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
     * @param lockType
     * @return
     * @throws SQLException
     */
    private LockTrace popLockTrace(LockTrace current, LockId lock)
            throws SQLException {
        if (current.getLockNode().equals(lock)) {
            return current.getParent();
        } else {
            return pushLockTrace(popLockTrace(current.getParent(), lock),
                    current.getLockNode(), current.getTrace());
        }
    }

    /**
     * Push a new lock onto the current lock trace, and return that lock trace.
     *
     * @param current
     * @param lock
     * @param lockType
     * @return
     * @throws SQLException
     */
    private LockTrace pushLockTrace(LockTrace current, LockId lock, long trace)
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
            lockTrace = LockTrace.newRootLockTrace(lockTraceIdSeq++, lock,
                    trace);
            traceRoots.put(trace, lockTrace);
        } else {
            // Use the cached version if we've got it
            for (LockTrace child : current.children()) {
                if (child.matches(lock, trace)) {
                    return child;
                }
            }
            lockTrace = current.pushLockTrace(lockTraceIdSeq++, lock, trace);
        }
        // Insert a new lock. We should have already returned from the method
        // if one had been found already.
        PreparedStatement st = statements.get(LOCK_TRACE);
        // INSERT INTO LOCKTRACE (Id,Lock,Trace,Parent) VALUES(?,?,?,?)
        int idx = 1;
        st.setLong(idx++, lockTrace.getId());
        st.setLong(idx++, lock.getId());
        st.setString(idx++, lock.getType().getFlag());
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
        LockId intr = new LockId(lock, LockType.INTRINSIC);
        LockId util = new LockId(lock, LockType.UTIL);
        List<LockTrace> list = lockTraces.remove(intr);
        if (list != null) {
            for (LockTrace l : list) {
                l.expunge();
            }
        }
        list = lockTraces.remove(util);
        if (list != null) {
            for (LockTrace l : list) {
                l.expunge();
            }
        }
        lockTraceRoots.remove(intr);
        lockTraceRoots.remove(util);
    }
}
