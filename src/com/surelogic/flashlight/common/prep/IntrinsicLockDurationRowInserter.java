package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.JDBCUtils;
import com.surelogic.common.logging.SLLogger;

public final class IntrinsicLockDurationRowInserter {
	private static final long FINAL_EVENT = Lock.FINAL_EVENT;
	private static final int LOCK_DURATION = 0;
	private static final int LOCKS_HELD = 1;
	private static final int THREAD_STATS = 2;
	private static final int LOCK_CYCLE = 3;
	private static final int INSERT_LOCK = 4;
	private static final boolean doInsert = AbstractPrep.doInsert;

	private static final String[] queries = {
			"INSERT INTO LOCKDURATION (InThread,Lock,Start,StartEvent,Stop,StopEvent,Duration,State) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
			"INSERT INTO LOCKSHELD (LockEvent,LockHeld,LockAcquired,InThread) VALUES (?, ?, ?, ?)",
			"INSERT INTO LOCKTHREADSTATS (LockEvent,Time,Blocking,Holding,Waiting) VALUES (?, ?, ?, ?, ?)",
			"INSERT INTO LOCKCYCLE (Component,LockHeld,LockAcquired,Count,FirstTime,LastTime) VALUES (?, ?, ?, ?, ?, ?)",
			"INSERT INTO LOCK (Id,TS,InThread,Trace,Lock,Type,State,Success,LockIsThis,LockIsClass) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" };
	private final PreparedStatement[] statements = new PreparedStatement[queries.length];
	private final int[] counts = new int[queries.length];

	static class State {
		IntrinsicLockDurationState _lockState = IntrinsicLockDurationState.IDLE;
		int _timesEntered = 0;

		// Info about the event that started us in this state
		long _id;
		long _lock;
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
			++_timesEntered;
		}

		public void lockAcquired() {
			--_timesEntered;
		}

	}

	static class ThreadState {
		Map<Long, State> lockToState = new HashMap<Long, State>();
		final Set<State> nonIdleLocks = new HashSet<State>();
		final Set<State> heldLocks = new HashSet<State>();

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
		public Collection<State> heldLocks() {
			return heldLocks;
		}

		public void update(final State mutableState, final long id,
				final long trace, final Timestamp time,
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
			mutableState._trace = trace;
			mutableState._time = time;
			mutableState._startEvent = startEvent;
			mutableState._lockState = lockState;
		}

	}

	private final Map<Long, ThreadState> f_threadToLockToState = new HashMap<Long, ThreadState>();
	private final Map<Long, IntrinsicLockDurationState> f_threadToStatus = new HashMap<Long, IntrinsicLockDurationState>();

	static class Edge extends DefaultEdge {
		private static final long serialVersionUID = 1L;
		final Long lockHeld;
		final Long lockAcquired;
		private Timestamp first;
		private Timestamp last;
		private long count;

		Edge(final Long held, final Long acq) {
			lockHeld = held;
			lockAcquired = acq;
		}

		public void setFirst(final Timestamp t) {
			if (first != null) {
				throw new IllegalStateException("Already set first time");
			}
			first = last = t;
			count = 1;
		}

		public void updateLast(final Timestamp time) {
			if ((time != null) && time.after(last)) {
				last = time;
				count++;
			}
		}

		public long getCount() {
			return count;
		}
	}

	static class EdgeFactory implements org.jgrapht.EdgeFactory<Long, Edge> {
		public Edge createEdge(final Long held, final Long acq) {
			return new Edge(held, acq);
		}
	}

	/**
	 * Vertices = locks Edge weight = # of times the edge appears
	 */
	private final DefaultDirectedGraph<Long, Edge> lockGraph = new DefaultDirectedGraph<Long, Edge>(
			new EdgeFactory());
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

		final CycleDetector<Long, Edge> detector = new CycleDetector<Long, Edge>(
				lockGraph);
		if (detector.detectCycles()) {
			final PreparedStatement f_cyclePS = statements[LOCK_CYCLE];

			final StrongConnectivityInspector<Long, Edge> inspector = new StrongConnectivityInspector<Long, Edge>(
					lockGraph);
			int compId = 0;
			for (final DirectedSubgraph<Long, Edge> comp : inspector
					.stronglyConnectedSubgraphs()) {
				for (final Edge e : comp.edgeSet()) {
					// Should only be output once
					int idx = 1;
					f_cyclePS.setInt(idx++, compId);
					f_cyclePS.setLong(idx++, e.lockHeld);
					f_cyclePS.setLong(idx++, e.lockAcquired);
					f_cyclePS.setLong(idx++, e.count);
					f_cyclePS.setTimestamp(idx++, e.first);
					f_cyclePS.setTimestamp(idx++, e.last);
					if (doInsert) {
					f_cyclePS.addBatch();
					if (++counts[LOCK_CYCLE] == 10000) {
						f_cyclePS.executeBatch();
						counts[LOCK_CYCLE] = 0;
					}
					}
				}
				compId++;
			}
		}
		for (int i = 0; i < queries.length; i++) {
			if (counts[i] > 0) {
				statements[i].executeBatch();
				counts[i] = 0;
			}
		}
	}

	private void handleNonIdleFinalState(final Timestamp endTime)
			throws SQLException {
		boolean createdEvent = false;
		int blocking = 0, holding = 0, waiting = 0;
		for (final Entry<Long, IntrinsicLockDurationState> e : f_threadToStatus
				.entrySet()) {
			final Long thread = e.getKey();
			System.out.println("Thread " + thread + " : " + e.getValue());
			switch (e.getValue()) {
			case BLOCKING:
				blocking++;
				break;
			case HOLDING:
				holding++;
				break;
			case WAITING:
				waiting++;
				break;
			default:
			}

			final ThreadState lockToState = f_threadToLockToState.get(thread);
			if (lockToState != null) {
				for (final State state : lockToState.nonIdleLocks()) {
					System.out.println("\tLock " + state.getLock() + " : "
							+ state.getLockState());
					if (!createdEvent) {
						createdEvent = true;
						insertLock(true, endTime, thread, state.getTrace(), /*
																			 * src
																			 * is
																			 * nonsense
																			 */
						state.getLock(), LockType.INTRINSIC,
								LockState.AFTER_RELEASE, true, false, false);
					}
					if (state.getLockState() == IntrinsicLockDurationState.BLOCKING) {
						noteHeldLocks(FINAL_EVENT, endTime, thread, state
								.getLock(), lockToState);
					}
					recordStateDuration(thread, state.getLock(), state
							.getTime(), state.getId(), endTime, FINAL_EVENT,
							state.getLockState());
				}
			}
		}
		if (createdEvent) {
			recordThreadStats(FINAL_EVENT, endTime, blocking, holding, waiting);
		}
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
			final LockState startEvent,
			final IntrinsicLockDurationState lockState,
			final ThreadState lockToState) {
		final IntrinsicLockDurationState oldLockState = state.getLockState();
		lockToState.update(state, id, trace, time, startEvent, lockState);
		updateThreadStatus(id, time, inThread, oldLockState, lockState);
	}

	private void updateThreadStatus(final long id, final Timestamp time,
			final long inThread, final IntrinsicLockDurationState oldLockState,
			final IntrinsicLockDurationState newLockState) {
		int blocking = 0, holding = 0, waiting = 0;
		boolean found = false;
		for (final Entry<Long, IntrinsicLockDurationState> e : f_threadToStatus
				.entrySet()) {
			final IntrinsicLockDurationState thisStatus = e.getValue();
			// Check if it's the current thread
			final long thisThread = e.getKey();
			if (inThread == thisThread) {
				found = true;
				e.setValue(computeThisThreadStatus(thisStatus, oldLockState,
						newLockState, inThread));
			}
			switch (thisStatus) {
			case BLOCKING:
				blocking++;
				break;
			case HOLDING:
				holding++;
				break;
			case WAITING:
				waiting++;
				break;
			default:
				continue;
			}
		}
		if (!found) {
			// insert the entry
			final IntrinsicLockDurationState newState = computeThisThreadStatus(
					IntrinsicLockDurationState.IDLE, oldLockState,
					newLockState, inThread);
			f_threadToStatus.put(inThread, newState);
			switch (newState) {
			case BLOCKING:
				blocking++;
				break;
			case HOLDING:
				holding++;
				break;
			case WAITING:
				waiting++;
				break;
			default:
			}
		}
		recordThreadStats(id, time, blocking, holding, waiting);
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
			final long trace, final long lock, final LockState lockEvent,
			final boolean success) {
		// A failed release attempt changes no states.
		if ((lockEvent == LockState.AFTER_RELEASE) && !success) {
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
				updateState(state, id, time, inThread, trace, lockEvent,
						IntrinsicLockDurationState.BLOCKING, lockToState);
			} else {
				logBadEventTransition(inThread, lock, lockEvent, state);
			}
			break;
		case BLOCKING:
			handlePossibleLockAcquire(id, time, inThread, trace, lock,
					lockEvent, LockState.AFTER_ACQUISITION, lockToState, state,
					success);
			break;
		case HOLDING:
			handleEventWhileHolding(id, time, inThread, trace, lock, lockEvent,
					state, lockToState);
			break;
		case WAITING:
			handlePossibleLockAcquire(id, time, inThread, trace, lock,
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
			final LockState lockEvent, final State state,
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
			updateState(state, id, time, inThread, trace, lockEvent,
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
				recordStateDuration(inThread, lock, state.getTime(), state
						.getId(), time, id, state.getLockState());
				updateState(state, id, time, inThread, trace, lockEvent,
						newState, lockToState);
			}
			break;
		default:
			SLLogger.getLogger().log(Level.SEVERE,
					I18N.err(104, lockEvent.toString()));
		}
	}

	private void handlePossibleLockAcquire(final long id, final Timestamp time,
			final long inThread, final long trace, final long lock,
			final LockState lockEvent, final LockState eventToMatch,
			final ThreadState lockToState, final State state,
			final boolean success) {
		if (lockEvent == eventToMatch) {
			assert state.getTimesEntered() >= 0;
			if (success) {
				noteHeldLocks(id, time, inThread, lock, lockToState);
			}
			recordStateDuration(inThread, lock, state.getTime(), state.getId(),
					time, id, state.getLockState());
			updateState(state, id, time, inThread, trace, lockEvent,
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
				I18N.err(102, state.getLockState().toString(), lockEvent
						.toString(), lock, inThread));
	}

	private void noteHeldLocks(final long id, final Timestamp time,
			final long thread, final long lock, final ThreadState lockToState) {
		// Note what other locks are held at the time of this event
		for (final State state : lockToState.heldLocks()) {
			if (lock == state.getLock()) {
				continue; // Skip myself
			}
			insertHeldLock(id, time, state.getLock(), lock, thread);
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
			f_ps.setTimestamp(idx++, startTime);
			f_ps.setLong(idx++, startEvent);
			f_ps.setTimestamp(idx++, stopTime);
			f_ps.setLong(idx++, stopEvent);

			final long secs = (stopTime.getTime() / 1000)
					- (startTime.getTime() / 1000);
			final long nanos = stopTime.getNanos() - startTime.getNanos();

			f_ps.setLong(idx++, (1000000000 * secs) + nanos);
			f_ps.setString(idx++, state.toString());
			f_ps.addBatch();
			if (++counts[LOCK_DURATION] == 10000) {
				f_ps.executeBatch();
				counts[LOCK_DURATION] = 0;
			}
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKDURATION", e);
		}
	}

	private void insertHeldLock(final long eventId, final Timestamp time,
			final Long lock, final long acquired, final long thread) {
		final PreparedStatement f_heldLockPS = statements[LOCKS_HELD];
		try {
			int idx = 1;
			f_heldLockPS.setLong(idx++, eventId);
			f_heldLockPS.setLong(idx++, lock);
			f_heldLockPS.setLong(idx++, acquired);
			f_heldLockPS.setLong(idx++, thread);
			f_heldLockPS.addBatch();
			if (++counts[LOCKS_HELD] == 10000) {
				f_heldLockPS.executeBatch();
				counts[LOCKS_HELD] = 0;
			}
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: ILOCKSHELD",
					e);
		}
		lockGraph.addVertex(lock);

		final Long acq = acquired;
		lockGraph.addVertex(acq);

		Edge addedEdge = lockGraph.addEdge(lock, acq);
		if (addedEdge != null) {
			addedEdge.setFirst(time);
		} else {
			addedEdge = lockGraph.getEdge(lock, acq);
			addedEdge.updateLast(time);
		}
	}

	public void defineRWLock(final long id, final Long readLock,
			final Long writeLock, final Timestamp startTime) {
		// Add dependency edges between the read and write locks
		lockGraph.addVertex(readLock);
		lockGraph.addVertex(writeLock);
		final Edge addedEdge1 = lockGraph.addEdge(readLock, writeLock);
		if (addedEdge1 != null) {
			addedEdge1.setFirst(startTime);
		}
		final Edge addedEdge2 = lockGraph.addEdge(writeLock, readLock);
		if (addedEdge2 != null) {
			addedEdge2.setFirst(startTime);
		}
	}

	private void recordThreadStats(final long eventId, final Timestamp t,
			final int blocking, final int holding, final int waiting) {
		final PreparedStatement f_threadStatusPS = statements[THREAD_STATS];
		try {
			int idx = 1;
			f_threadStatusPS.setLong(idx++, eventId);
			f_threadStatusPS.setTimestamp(idx++, t);
			f_threadStatusPS.setInt(idx++, blocking);
			f_threadStatusPS.setInt(idx++, holding);
			f_threadStatusPS.setInt(idx++, waiting);
			f_threadStatusPS.addBatch();
			if (++counts[THREAD_STATS] == 10000) {
				f_threadStatusPS.executeBatch();
				counts[THREAD_STATS] = 0;
			}
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKTHREADSTATS", e);
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
		for (final ThreadState e : f_threadToLockToState.values()) {
			e.gcLock(key);
		}
		// TODO Clean up lock graph?
	}

	private long f_lockId = 0;

	// Only called here and from Lock
	long insertLock(final boolean finalEvent, final Timestamp time,
			final long inThread, final long trace, final long lock,
			final LockType lockType, final LockState lockState,
			final Boolean success, final Boolean lockIsThis,
			final Boolean lockIsClass) throws SQLException {
		final PreparedStatement ps = statements[INSERT_LOCK];
		int idx = 1;
		ps.setLong(idx++, finalEvent ? Lock.FINAL_EVENT : ++f_lockId);
		ps.setTimestamp(idx++, time);
		ps.setLong(idx++, inThread);
		ps.setLong(idx++, trace);
		ps.setLong(idx++, lock);
		ps.setString(idx++, lockType.getFlag());
		ps.setString(idx++, lockState.toString().replace('_', ' '));
		JDBCUtils.setNullableBoolean(idx++, ps, success);
		JDBCUtils.setNullableBoolean(idx++, ps, lockIsThis);
		JDBCUtils.setNullableBoolean(idx++, ps, lockIsClass);
		ps.addBatch();
		if (++counts[INSERT_LOCK] == 10000) {
			ps.executeBatch();
			counts[INSERT_LOCK] = 0;
		}
		return f_lockId;
	}
}
