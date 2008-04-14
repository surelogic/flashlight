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

	static class Event {
		long id;
		Timestamp time;
		IntrinsicLockState lockState;
	}

	private final Map<Long, Map<Long, Event>> f_threadToLockToEvent = new HashMap<Long, Map<Long, Event>>();

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
			f_threadToLockToEvent.clear();
		}
	}

	private Map<Long, Event> getLockToEventMap(long inThread) {
		Map<Long, Event> lockToEvent = f_threadToLockToEvent.get(inThread);
		if (lockToEvent == null) {
			lockToEvent = new HashMap<Long, Event>();
			f_threadToLockToEvent.put(inThread, lockToEvent);
		}
		return lockToEvent;
	}
	
	private Event getEvent(Map<Long,Event> lockToEvent, long lock) {
		Event event = lockToEvent.get(lock);
		if (event == null) {
			event = new Event();
			lockToEvent.put(lock, event);
		}
		return event;
	}

	private void setEvent(Event mutableEvent, long id, Timestamp time,
			IntrinsicLockState lockState) {
		mutableEvent.id = id;
		mutableEvent.time = time;
		mutableEvent.lockState = lockState;
	}

	public void event(int runId, long id, Timestamp time, long inThread,
			long lock, IntrinsicLockState lockState) {
		final Map<Long, Event> lockToEvent = getLockToEventMap(inThread);
        for(Map.Entry<Long, Event> e : lockToEvent.entrySet()) {
        	IntrinsicLockState state = e.getValue().lockState;
        	if (state.isLockHeld()) {
        		insertHeldLock(runId, id, e.getKey());
        	}
        }		
		
		final Event event = getEvent(lockToEvent, lock);
		if (lockState == IntrinsicLockState.BEFORE_ACQUISITION) {
			// event information is saved below
			// FIX what about re-entrant locks?
		} else if (lockState == IntrinsicLockState.AFTER_ACQUISITION) {
			if (event.lockState == IntrinsicLockState.BEFORE_ACQUISITION) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.BLOCKING);
			} else {
				LogStatus.createErrorStatus(0, event.lockState
						+ " cannot proceed " + lockState + " for lock " + lock
						+ " in thread " + inThread);
			}
		} else if (lockState == IntrinsicLockState.BEFORE_WAIT) {
			if (event.lockState == IntrinsicLockState.AFTER_ACQUISITION) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.HOLDING);
			} else {
				LogStatus.createErrorStatus(0, event.lockState
						+ " cannot proceed " + lockState + " for lock " + lock
						+ " in thread " + inThread);
			}
		} else if (lockState == IntrinsicLockState.AFTER_WAIT) {
			if (event.lockState == IntrinsicLockState.BEFORE_WAIT) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.WAITING);
			} else {
				LogStatus.createErrorStatus(0, event.lockState
						+ " cannot proceed " + lockState + " for lock " + lock
						+ " in thread " + inThread);
			}
		} else if (lockState == IntrinsicLockState.AFTER_RELEASE) {
			if (event.lockState == IntrinsicLockState.AFTER_ACQUISITION
					|| event.lockState == IntrinsicLockState.AFTER_WAIT) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.HOLDING);
				// FIX should be able to remove the lock state
			} else {
				LogStatus.createErrorStatus(0, event.lockState
						+ " cannot proceed " + lockState + " for lock " + lock
						+ " in thread " + inThread);
			}
		} else {
			LogStatus.createErrorStatus(0, "Unknown intrinsic lock state "
					+ lockState);
			return;
		}
		setEvent(event, id, time, lockState);
	}

	private void insert(int runId, long inThread, long lock,
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
