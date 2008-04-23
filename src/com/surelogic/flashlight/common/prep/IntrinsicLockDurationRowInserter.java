package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

import com.surelogic.common.logging.LogStatus;
import com.surelogic.common.logging.SLLogger;

public final class IntrinsicLockDurationRowInserter {
	private static final int LOCK_DURATION = 0;
	private static final int LOCKS_HELD = 1;
	private static final int THREAD_STATS = 2;
	private static final int LOCK_CYCLE = 3;
	
    private static final String[] queries = {
	  "INSERT INTO ILOCKDURATION VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
	  "INSERT INTO ILOCKSHELD VALUES (?, ?, ?, ?, ?)",
      "INSERT INTO ILOCKTHREADSTATS VALUES (?, ?, ?, ?, ?, ?)",
	  "INSERT INTO ILOCKCYCLE VALUES (?, ?)",
    };
    private static final PreparedStatement[] statements = 
    	new PreparedStatement[queries.length];
	
	static class State {
		IntrinsicLockDurationState lockState = IntrinsicLockDurationState.IDLE;
		int timesEntered = 0;
		
		// Info about the event that started us in this state
		long id;
		Timestamp time;
		IntrinsicLockState startEvent;
	}

	private final Map<Long, Map<Long, State>> f_threadToLockToState = 
		new HashMap<Long, Map<Long, State>>();
    private final Map<Long, IntrinsicLockDurationState> f_threadToStatus = 
    	new HashMap<Long, IntrinsicLockDurationState>();

    
    static class HeldLockRange {
    	final Long lock;
    	final Timestamp first;
    	Timestamp last;
    	
    	HeldLockRange(Long l, Timestamp t) {
    		lock = l;
    		first = last = t;
    	}

		public void updateLast(Timestamp time) {
			if (time != null && time.after(last)) {
				last = time;
			}
		}
    }
    /**
     * Vertices = locks
     * Edges = threads doing the acquire
     * Edge weight = # of times the edge appears
     */
    private final DefaultDirectedWeightedGraph<Long, Object> lockGraph = 
    	new DefaultDirectedWeightedGraph<Long, Object>(Object.class);
    private final Map<Long,HeldLockRange> heldLocks = new HashMap<Long,HeldLockRange>();
    private boolean flushed = false;
    
	public IntrinsicLockDurationRowInserter(final Connection c)
			throws SQLException {
		int i=0;
		for (String q : queries) {
			statements[i++] = c.prepareStatement(q);
		}
	}

	public void flush(final int runId) throws SQLException {
		if (flushed) {
			return;
		}
		flushed = true;

		CycleDetector<Long, Object> detector = new CycleDetector<Long, Object>(lockGraph);
		if (detector.detectCycles()) {
			StrongConnectivityInspector<Long, Object> inspector = 
				new StrongConnectivityInspector<Long, Object>(lockGraph);
			Set<Long> cycled = new HashSet<Long>();
			for(Set<Long> comp : inspector.stronglyConnectedSets()) {
				cycled.addAll(comp);
			}

			final PreparedStatement f_cyclePS = statements[LOCK_CYCLE];
			for(Long lockId : cycled) {
				f_cyclePS.setInt(1, runId);
				f_cyclePS.setLong(2, lockId);
				f_cyclePS.executeUpdate();
			}
		}
	}
	
	public void close() throws SQLException {	
		for(int i=0; i<statements.length; i++) {
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
	
	private State getState(Map<Long,State> lockToState, long lock) {
		State event = lockToState.get(lock);
		if (event == null) {
			event = new State();
			lockToState.put(lock, event);
		}
		return event;
	}

	private void updateState(State mutableState, int run, long id, Timestamp time, 
			                 long inThread, IntrinsicLockState startEvent, 
			                 IntrinsicLockDurationState lockState) {
		IntrinsicLockDurationState oldLockState = mutableState.lockState;
		mutableState.id = id;
		mutableState.time = time;
		mutableState.startEvent = startEvent;
		mutableState.lockState = lockState;

		updateThreadStatus(run, id, time, inThread, oldLockState, lockState);
	}

	private void updateThreadStatus(int run, long id, Timestamp time, long inThread, 
			                        IntrinsicLockDurationState oldLockState,
			                        IntrinsicLockDurationState newLockState) {
		int blocking = 0, holding = 0, waiting = 0;
		boolean found = false;
		for(Entry<Long, IntrinsicLockDurationState> e : f_threadToStatus.entrySet()) {
		    IntrinsicLockDurationState thisStatus = e.getValue();
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
			IntrinsicLockDurationState newState = 
				computeThisThreadStatus(IntrinsicLockDurationState.IDLE, 
						                oldLockState, newLockState, inThread);
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
	private IntrinsicLockDurationState 
	computeThisThreadStatus(IntrinsicLockDurationState threadState,
     		                IntrinsicLockDurationState oldLockState, 
			                IntrinsicLockDurationState newLockState, 
			                long inThread) {
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

	private IntrinsicLockDurationState 
	computeRunningThreadStatus(IntrinsicLockDurationState newLockState, long inThread) {
		if (newLockState == IntrinsicLockDurationState.IDLE) {
			// Need to check the status on the other locks
			final Map<Long, State> lockToState = getLockToStateMap(inThread);
			for(Entry<Long, State> e : lockToState.entrySet()) {
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
			long lock, IntrinsicLockState lockEvent) {
		final Map<Long, State> lockToState = getLockToStateMap(inThread);		
		final State state = getState(lockToState, lock);
		switch (state.lockState) {
		case IDLE:
			// Only legal transition is BA->Block
			assert state.timesEntered == 0;
			if (lockEvent == IntrinsicLockState.BEFORE_ACQUISITION) {
				// No need to record idle time
				updateState(state, runId, id, time, inThread,
						    lockEvent, IntrinsicLockDurationState.BLOCKING);
			} else {
				logBadEvent(inThread, lock, lockEvent, state);
			}
			break;
		case BLOCKING:
			// Only legal transition is AA->Hold+1
			handlePossibleLockAcquire(runId, id, time, inThread, lock, lockEvent,
	                                  IntrinsicLockState.AFTER_ACQUISITION, lockToState, state);
			break;
		case HOLDING:
			handleEventWhileHolding(runId, id, time, inThread, lock, lockEvent, state);
			break;
		case WAITING:			
			// Only legal transition is AW->Hold+1
			handlePossibleLockAcquire(runId, id, time, inThread, lock, lockEvent,
	                                  IntrinsicLockState.AFTER_WAIT, lockToState, state);
			break;
		default:
			LogStatus.createErrorStatus(0, "Unexpected lock state: "+state.lockState);
		}
	}

	/**
	 * Legal transitions:
	 * BA: Hold
	 * AA: Hold+1
	 * BW: Wait-1
	 * AR: Idle-1 if timesEntered = 1, otherwise Hold-1
	 */
	private void handleEventWhileHolding(int runId, long id, Timestamp time, long inThread, 
			                             long lock, IntrinsicLockState lockEvent, State state) {
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
			noteStateTransition(runId, id, time, inThread, lock, lockEvent, state,
					            IntrinsicLockDurationState.WAITING);
			break;
		case AFTER_WAIT:
			logBadEvent(inThread, lock, lockEvent, state);
			break;
		case AFTER_RELEASE:
			state.timesEntered--;			
			
			final IntrinsicLockDurationState newState = 
				state.timesEntered == 0 ? IntrinsicLockDurationState.IDLE :
					                      IntrinsicLockDurationState.HOLDING;
			noteStateTransition(runId, id, time, inThread, lock, lockEvent, state,
					            newState);
			break;
		default: 
			LogStatus.createErrorStatus(0, "Unexpected lock event: "+lockEvent);
		}
	}
	
	private void handlePossibleLockAcquire(int runId, long id, Timestamp time, long inThread,
			                               long lock, IntrinsicLockState lockEvent,
			                               final IntrinsicLockState eventToMatch,
			                               Map<Long, State> lockToState, State state) {
		if (lockEvent == eventToMatch) {
			assert state.timesEntered >= 0;
			noteHeldLocks(runId, id, time, inThread, lock, lockToState);
			noteStateTransition(runId, id, time, inThread, lock, 
					            lockEvent, state, IntrinsicLockDurationState.HOLDING);		
			state.timesEntered++;
		} else {
			logBadEvent(inThread, lock, lockEvent, state);
		}
	}

	private void noteStateTransition(int runId, long id, Timestamp time, long inThread, 
			                         long lock, IntrinsicLockState lockEvent, State state,
			                         IntrinsicLockDurationState newState) {
		recordStateDuration(runId, inThread, lock, state.time, state.id, time, id,
			                state.lockState);
		updateState(state, runId, id, time, inThread, lockEvent, newState);
	}
	
	private void logBadEvent(long inThread, long lock,
			IntrinsicLockState lockEvent, final State state) {
		LogStatus.createErrorStatus(0, state.lockState
				+ " has no transition for " + lockEvent + " for lock " + lock
				+ " in thread " + inThread);
	} 

	private void noteHeldLocks(int runId, long id, Timestamp time, long thread, long lock,
			final Map<Long, State> lockToState) {
		// Note what other locks are held at the time of this event 
		for(Map.Entry<Long, State> e : lockToState.entrySet()) {
			long otherLock = e.getKey();
			if (lock == otherLock) {
				continue; // Skip myself
			}
			IntrinsicLockDurationState state = e.getValue().lockState;
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
			
			long secs  = (stopTime.getTime() / 1000) - (startTime.getTime() / 1000);
			long nanos = stopTime.getNanos() - startTime.getNanos();
			
			f_ps.setLong(8, (1000000000 * secs) + nanos);
			f_ps.setString(9, state.toString());
			f_ps.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKDURATION", e);
		}
	}
	
	private void insertHeldLock(int runId, long eventId, Timestamp time, Long lock, long acquired, long thread) {
		final PreparedStatement f_heldLockPS = statements[LOCKS_HELD];
		try {
			f_heldLockPS.setInt(1, runId);
			f_heldLockPS.setLong(2, eventId);
			f_heldLockPS.setLong(3, lock);
			f_heldLockPS.setLong(4, acquired);
			f_heldLockPS.setLong(5, thread);
			f_heldLockPS.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKSHELD", e);
		}				
		boolean addedV1 = lockGraph.addVertex(lock);
	    updateHeldLockRange(addedV1, lock, time);
		
		final Long acq = acquired;				
		lockGraph.addVertex(acq);		
		 
	    final Long t = thread; 
		boolean addedEdge = lockGraph.addEdge(lock, acq, t);
		if (addedEdge) {
			lockGraph.setEdgeWeight(t, 1.0);
		} else {
			double wt = lockGraph.getEdgeWeight(t);
			lockGraph.setEdgeWeight(t, wt + 1.0);
		}
	}
	
	private void updateHeldLockRange(boolean added, Long lock, Timestamp time) {
		HeldLockRange range = added ? null : heldLocks.get(lock);
		if (added || range == null) {
			range = new HeldLockRange(lock, time);
			heldLocks.put(lock, range);
		} else {			
			range.updateLast(time);
		}		
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
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKTHREADSTATS", e);
		}
	}
	
	/*
select distinct lockheld, lockacquired
FROM ilocksheld 

select distinct l1.lockheld, l2.lockheld, l2.lockacquired
FROM ilocksheld AS l1, ilocksheld AS l2
WHERE l1.inthread <> l2.inthread
AND l1.lockacquired = l2.lockheld
AND l2.lockacquired = l1.lockheld

select distinct l1.lockheld, l2.lockheld, l3.lockheld, l3.lockacquired
FROM ilocksheld AS l1, ilocksheld AS l2, ilocksheld AS l3
WHERE l1.inthread <> l2.inthread
AND l2.inthread <> l3.inthread
AND l1.inthread <> l3.inthread
AND l1.lockacquired = l2.lockheld
AND l2.lockacquired = l3.lockheld
AND l3.lockacquired = l1.lockheld

select * from ilockduration
where state = 'BLOCKING'
order by duration desc

select run, inthread, lock, sum(duration) as blocktime
from ilockduration
where state = 'BLOCKING'
group by run, inthread, lock
order by blocktime desc
	 */
}
