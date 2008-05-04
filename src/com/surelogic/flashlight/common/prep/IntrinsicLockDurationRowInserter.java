package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;

import com.surelogic.common.logging.LogStatus;
import com.surelogic.common.logging.SLLogger;

public final class IntrinsicLockDurationRowInserter {
  private static final long FINAL_EVENT = Lock.FINAL_EVENT;
	private static final int LOCK_DURATION = 0;
	private static final int LOCKS_HELD = 1;
	private static final int THREAD_STATS = 2;
	private static final int LOCK_CYCLE = 3;

	private static final String[] queries = {
			"INSERT INTO LOCKDURATION (Run,InThread,Lock,Start,StartEvent,Stop,StopEvent,Duration,State) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
			"INSERT INTO LOCKSHELD (Run,LockEvent,LockHeld,LockAcquired,InThread) VALUES (?, ?, ?, ?, ?)",
			"INSERT INTO LOCKTHREADSTATS (Run,LockEvent,Time,Blocking,Holding,Waiting) VALUES (?, ?, ?, ?, ?, ?)",
			"INSERT INTO LOCKCYCLE (Run,Component,LockHeld,LockAcquired,Count,FirstTime,LastTime) VALUES (?, ?, ?, ?, ?, ?, ?)", };
	private static final PreparedStatement[] statements = new PreparedStatement[queries.length];

	static class State {
		IntrinsicLockDurationState lockState = IntrinsicLockDurationState.IDLE;
		int timesEntered = 0;

		// Info about the event that started us in this state
		long id;
		Timestamp time;
		LockState startEvent;
	}

	private final Map<Long, Map<Long, State>> f_threadToLockToState = new HashMap<Long, Map<Long, State>>();
	private final Map<Long, IntrinsicLockDurationState> f_threadToStatus = new HashMap<Long, IntrinsicLockDurationState>();

	static class Edge extends DefaultEdge {
		private static final long serialVersionUID = 1L;
		final Long lockHeld;
		final Long lockAcquired;
		private Timestamp first;
		private Timestamp last;
		private long count;

		Edge(Long held, Long acq) {
			lockHeld = held;
			lockAcquired = acq;
		}

		public void setFirst(Timestamp t) {
			if (first != null) {
				throw new IllegalStateException("Already set first time");
			}
			first = last = t;
			count = 1;
		}

		public void updateLast(Timestamp time) {
			if (time != null && time.after(last)) {
				last = time;
				count++;
			}
		}

		public long getCount() {
			return count;
		}
	}

	static class EdgeFactory implements org.jgrapht.EdgeFactory<Long, Edge> {
		public Edge createEdge(Long held, Long acq) {
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

	public void flush(final int runId, final Timestamp endTime) throws SQLException {
		if (flushed) {
			return;
		}
		flushed = true;

		handleNonIdleFinalState(runId, endTime);
		
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
					f_cyclePS.setInt(1, runId);
					f_cyclePS.setInt(2, compId);
					f_cyclePS.setLong(3, e.lockHeld);
					f_cyclePS.setLong(4, e.lockAcquired);
					f_cyclePS.setLong(5, e.count);
					f_cyclePS.setTimestamp(6, e.first);
					f_cyclePS.setTimestamp(7, e.last);
					f_cyclePS.executeUpdate();
				}
				compId++;
			}
		}
	}

  private void handleNonIdleFinalState(final int runId, final Timestamp endTime) {
    boolean createdEvent = false;    
    int blocking = 0, holding = 0, waiting = 0;
    for(Entry<Long, IntrinsicLockDurationState> e : f_threadToStatus.entrySet()) {
      final Long thread = e.getKey();
      System.out.println("Thread "+thread+" : "+e.getValue());
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
      
      final Map<Long,State> lockToState = f_threadToLockToState.get(thread);
      if (lockToState != null) {
        for(Entry<Long,State> e2 : lockToState.entrySet()) {
          final long lock = e2.getKey();
          final State state = e2.getValue();
          if (state.lockState != IntrinsicLockDurationState.IDLE) {
            System.out.println("\tLock "+lock+" : "+state.lockState);
            if (!createdEvent) {
              createdEvent = true;
              Lock.insert(runId, FINAL_EVENT, endTime, thread, lock, 0, /* src is nonsense */
                          lock, LockType.INTRINSIC, 
                          LockState.AFTER_RELEASE, true, false, false);
            }            
            if (state.lockState == IntrinsicLockDurationState.BLOCKING) {
              noteHeldLocks(runId, FINAL_EVENT, endTime, thread, lock, lockToState);            
            }
            recordStateDuration(runId, thread, lock, state.time, state.id, 
                                endTime, FINAL_EVENT, state.lockState);
          }
        }
      }
    }
    if (createdEvent) {
      recordThreadStats(runId, FINAL_EVENT, endTime, blocking, holding, waiting);
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

	private Map<Long, State> getLockToStateMap(long inThread) {
		Map<Long, State> lockToState = f_threadToLockToState.get(inThread);
		if (lockToState == null) {
			lockToState = new HashMap<Long, State>();
			f_threadToLockToState.put(inThread, lockToState);
		}
		return lockToState;
	}

	private State getState(Map<Long, State> lockToState, long lock) {
		State event = lockToState.get(lock);
		if (event == null) {
			event = new State();
			lockToState.put(lock, event);
		}
		return event;
	}

	private void updateState(State mutableState, int run, long id,
			Timestamp time, long inThread, LockState startEvent,
			IntrinsicLockDurationState lockState) {
		final IntrinsicLockDurationState oldLockState = mutableState.lockState;
		mutableState.id = id;
		mutableState.time = time;
		mutableState.startEvent = startEvent;
		mutableState.lockState = lockState;

		updateThreadStatus(run, id, time, inThread, oldLockState, lockState);
	}

	private void updateThreadStatus(int run, long id, Timestamp time,
			long inThread, IntrinsicLockDurationState oldLockState,
			IntrinsicLockDurationState newLockState) {
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
		recordThreadStats(run, id, time, blocking, holding, waiting);
	}

	/**
	 * Update this thread's status, based on an event on one of the locks
	 */
	private IntrinsicLockDurationState computeThisThreadStatus(
			IntrinsicLockDurationState threadState,
			IntrinsicLockDurationState oldLockState,
			IntrinsicLockDurationState newLockState, long inThread) {
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
			IntrinsicLockDurationState newLockState, long inThread) {
		if (newLockState == IntrinsicLockDurationState.IDLE) {
			// Need to check the status on the other locks
			final Map<Long, State> lockToState = getLockToStateMap(inThread);
			for (final Entry<Long, State> e : lockToState.entrySet()) {
				final State state = e.getValue();
				assert state.lockState.isRunning();
				if (state.lockState != IntrinsicLockDurationState.IDLE) {
					return state.lockState;
				}
			}
			// Otherwise idle
		}
		return newLockState;
	}

	public void event(int runId, long id, Timestamp time, long inThread,
			long lock, LockState lockEvent, boolean success) {
		// A failed release attempt changes no states.
		if ((lockEvent == LockState.AFTER_RELEASE) && !success) {
			return;
		}
		final Map<Long, State> lockToState = getLockToStateMap(inThread);
		final State state = getState(lockToState, lock);
		switch (state.lockState) {
		case IDLE:
			// Only legal transition is BA->Block
			assert state.timesEntered == 0;
			if (lockEvent == LockState.BEFORE_ACQUISITION) {
				// No need to record idle time
				updateState(state, runId, id, time, inThread, lockEvent,
						IntrinsicLockDurationState.BLOCKING);
			} else {
				logBadEvent(inThread, lock, lockEvent, state);
			}
			break;
		case BLOCKING:
			handlePossibleLockAcquire(runId, id, time, inThread, lock,
					lockEvent, LockState.AFTER_ACQUISITION, lockToState, state,
					success);
			break;
		case HOLDING:
			handleEventWhileHolding(runId, id, time, inThread, lock, lockEvent,
					state);
			break;
		case WAITING:
			handlePossibleLockAcquire(runId, id, time, inThread, lock,
					lockEvent, LockState.AFTER_WAIT, lockToState, state, true);
			break;
		default:
			LogStatus.createErrorStatus(0, "Unexpected lock state: "
					+ state.lockState);
		}
	}

	/**
	 * Legal transitions: BA: Hold AA: Hold+1 BW: Wait-1 AR: Idle-1 if
	 * timesEntered = 1, otherwise Hold-1
	 */
	private void handleEventWhileHolding(int runId, long id, Timestamp time,
			long inThread, long lock, LockState lockEvent, State state) {
		assert state.timesEntered > 0;
		switch (lockEvent) {
		case BEFORE_ACQUISITION:
			// Nothing to do right now
			break;
		case AFTER_ACQUISITION:
			state.timesEntered++;
			break;
		case BEFORE_WAIT:
			state.timesEntered--;
			noteStateTransition(runId, id, time, inThread, lock, lockEvent,
					state, IntrinsicLockDurationState.WAITING);
			break;
		case AFTER_WAIT:
			logBadEvent(inThread, lock, lockEvent, state);
			break;
		case AFTER_RELEASE:
			state.timesEntered--;

			final IntrinsicLockDurationState newState = state.timesEntered == 0 ? IntrinsicLockDurationState.IDLE
					: IntrinsicLockDurationState.HOLDING;
			noteStateTransition(runId, id, time, inThread, lock, lockEvent,
					state, newState);
			break;
		default:
			LogStatus.createErrorStatus(0, "Unexpected lock event: "
					+ lockEvent);
		}
	}

	private void handlePossibleLockAcquire(int runId, long id, Timestamp time,
			long inThread, long lock, LockState lockEvent,
			final LockState eventToMatch, Map<Long, State> lockToState,
			State state, boolean success) {
		if (lockEvent == eventToMatch) {
			assert state.timesEntered >= 0;
			if (success) {
				noteHeldLocks(runId, id, time, inThread, lock, lockToState);
			}
			noteStateTransition(runId, id, time, inThread, lock, lockEvent,
					state, success ? IntrinsicLockDurationState.HOLDING
							: IntrinsicLockDurationState.IDLE);
			state.timesEntered++;
		} else {
			logBadEvent(inThread, lock, lockEvent, state);
		}
	}

	private void noteStateTransition(int runId, long id, Timestamp time,
			long inThread, long lock, LockState lockEvent, State state,
			IntrinsicLockDurationState newState) {
		recordStateDuration(runId, inThread, lock, state.time, state.id, time,
				id, state.lockState);
		updateState(state, runId, id, time, inThread, lockEvent, newState);
	}

	private void logBadEvent(long inThread, long lock, LockState lockEvent,
			final State state) {
		LogStatus.createErrorStatus(0, state.lockState
				+ " has no transition for " + lockEvent + " for lock " + lock
				+ " in thread " + inThread);
	}

	private void noteHeldLocks(int runId, long id, Timestamp time, long thread,
			long lock, final Map<Long, State> lockToState) {
		// Note what other locks are held at the time of this event
		for (final Map.Entry<Long, State> e : lockToState.entrySet()) {
			final long otherLock = e.getKey();
			if (lock == otherLock) {
				continue; // Skip myself
			}
			final IntrinsicLockDurationState state = e.getValue().lockState;
			if (state == IntrinsicLockDurationState.HOLDING) {
				insertHeldLock(runId, id, time, e.getKey(), lock, thread);
			}
		}
	}

	private void recordStateDuration(int runId, long inThread, long lock,
			Timestamp startTime, long startEvent, Timestamp stopTime,
			long stopEvent, IntrinsicLockDurationState state) {
		final PreparedStatement f_ps = statements[LOCK_DURATION];
		try {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, inThread);
			f_ps.setLong(3, lock);
			f_ps.setTimestamp(4, startTime);
			f_ps.setLong(5, startEvent);
			f_ps.setTimestamp(6, stopTime);
			f_ps.setLong(7, stopEvent);

			final long secs = (stopTime.getTime() / 1000)
					- (startTime.getTime() / 1000);
			final long nanos = stopTime.getNanos() - startTime.getNanos();

			f_ps.setLong(8, (1000000000 * secs) + nanos);
			f_ps.setString(9, state.toString());
			f_ps.executeUpdate();
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKDURATION", e);
		}
	}

	private void insertHeldLock(int runId, long eventId, Timestamp time,
			Long lock, long acquired, long thread) {
		final PreparedStatement f_heldLockPS = statements[LOCKS_HELD];
		try {
			f_heldLockPS.setInt(1, runId);
			f_heldLockPS.setLong(2, eventId);
			f_heldLockPS.setLong(3, lock);
			f_heldLockPS.setLong(4, acquired);
			f_heldLockPS.setLong(5, thread);
			f_heldLockPS.executeUpdate();
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

  public void defineRWLock(int runId, long id, Long readLock, Long writeLock, Timestamp startTime) {
    // Add dependency edges between the read and write locks    
    lockGraph.addVertex(readLock);
    lockGraph.addVertex(writeLock);
    final Edge addedEdge1 = lockGraph.addEdge(readLock, writeLock);
    addedEdge1.setFirst(startTime);
    
    final Edge addedEdge2 = lockGraph.addEdge(writeLock, readLock);
    addedEdge2.setFirst(startTime);
  }
	
	private void recordThreadStats(int runId, long eventId, Timestamp t,
			int blocking, int holding, int waiting) {
		final PreparedStatement f_threadStatusPS = statements[THREAD_STATS];
		try {
			f_threadStatusPS.setInt(1, runId);
			f_threadStatusPS.setLong(2, eventId);
			f_threadStatusPS.setTimestamp(3, t);
			f_threadStatusPS.setInt(4, blocking);
			f_threadStatusPS.setInt(5, holding);
			f_threadStatusPS.setInt(6, waiting);
			f_threadStatusPS.executeUpdate();
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKTHREADSTATS", e);
		}
	}

	/*
	 * select distinct lockheld, lockacquired FROM ilocksheld
	 * 
	 * select distinct l1.lockheld, l2.lockheld, l2.lockacquired FROM ilocksheld
	 * AS l1, ilocksheld AS l2 WHERE l1.inthread <> l2.inthread AND
	 * l1.lockacquired = l2.lockheld AND l2.lockacquired = l1.lockheld
	 * 
	 * select distinct l1.lockheld, l2.lockheld, l3.lockheld, l3.lockacquired
	 * FROM ilocksheld AS l1, ilocksheld AS l2, ilocksheld AS l3 WHERE
	 * l1.inthread <> l2.inthread AND l2.inthread <> l3.inthread AND l1.inthread <>
	 * l3.inthread AND l1.lockacquired = l2.lockheld AND l2.lockacquired =
	 * l3.lockheld AND l3.lockacquired = l1.lockheld
	 * 
	 * select * from ilockduration where state = 'BLOCKING' order by duration
	 * desc
	 * 
	 * select run, inthread, lock, sum(duration) as blocktime from ilockduration
	 * where state = 'BLOCKING' group by run, inthread, lock order by blocktime
	 * desc
	 */
}
