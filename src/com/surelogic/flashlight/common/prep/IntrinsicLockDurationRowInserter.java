package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.common.logging.LogStatus;
import com.surelogic.common.logging.SLLogger;

public final class IntrinsicLockDurationRowInserter {

	private static final String f_psQ = "INSERT INTO ILOCKDURATION VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String f_heldLockQuery = "INSERT INTO ILOCKSHELD VALUES (?, ?, ?)";

	private PreparedStatement f_ps;
	private PreparedStatement f_heldLockPS;
	
	static class State {
		IntrinsicLockDurationState lockState = IntrinsicLockDurationState.IDLE;
		int timesEntered = 0;
		
		// Info about the event that started us in this state
		long id;
		Timestamp time;
		IntrinsicLockState startEvent;
	}

	private final Map<Long, Map<Long, State>> f_threadToLockToState = new HashMap<Long, Map<Long, State>>();

	public IntrinsicLockDurationRowInserter(final Connection c)
			throws SQLException {
		f_ps = c.prepareStatement(f_psQ);
		f_heldLockPS = c.prepareStatement(f_heldLockQuery);
	}

	public void close() throws SQLException {
		if (f_heldLockPS != null) {
			f_heldLockPS.close();
			f_heldLockPS = null;
		}
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
			f_threadToLockToState.clear();
		}
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

	private void updateState(State mutableState, long id, Timestamp time,
			IntrinsicLockState startEvent, IntrinsicLockDurationState lockState) {
		mutableState.id = id;
		mutableState.time = time;
		mutableState.startEvent = startEvent;
		mutableState.lockState = lockState;
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
				updateState(state, id, time, lockEvent, IntrinsicLockDurationState.BLOCKING);
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
			noteHeldLocks(runId, id, lock, lockToState);
			noteStateTransition(runId, id, time, inThread, lock, lockEvent, state,
					            IntrinsicLockDurationState.HOLDING);		
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
		updateState(state, id, time, lockEvent, newState);
	}
	
	private void logBadEvent(long inThread, long lock,
			IntrinsicLockState lockEvent, final State state) {
		LogStatus.createErrorStatus(0, state.lockState
				+ " has no transition for " + lockEvent + " for lock " + lock
				+ " in thread " + inThread);
	} 

	private void noteHeldLocks(int runId, long id, long lock,
			final Map<Long, State> lockToState) {
		// Note what other locks are held at the time of this event 
        for(Map.Entry<Long, State> e : lockToState.entrySet()) {
        	long otherLock = e.getKey();
        	if (lock == otherLock) {
        		continue; // Skip myself
        	}
        	IntrinsicLockDurationState state = e.getValue().lockState;
        	if (state == IntrinsicLockDurationState.HOLDING) {
        		insertHeldLock(runId, id, e.getKey());
        	}
        }
	}

	private void recordStateDuration(int runId, long inThread, long lock,
			Timestamp startTime, long startEvent, Timestamp stopTime,
			long stopEvent, IntrinsicLockDurationState state) {
		try {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, inThread);
			f_ps.setLong(3, lock);
			f_ps.setTimestamp(4, startTime);
			f_ps.setLong(5, startEvent);
			f_ps.setTimestamp(6, stopTime);
			f_ps.setLong(7, stopEvent);
			f_ps.setString(8, state.toString());
			f_ps.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKDURATION", e);
		}
	}
	
	private void insertHeldLock(int runId, long eventId, Long lock) {
		try {
			f_heldLockPS.setInt(1, runId);
			f_heldLockPS.setLong(2, eventId);
			f_heldLockPS.setLong(3, lock);
			f_heldLockPS.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Insert failed: ILOCKSHELD", e);
		}
	}
}
