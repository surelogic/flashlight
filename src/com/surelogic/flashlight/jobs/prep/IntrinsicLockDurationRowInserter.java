package com.surelogic.flashlight.jobs.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.flashlight.FLog;

public final class IntrinsicLockDurationRowInserter {

	private static final String f_psQ = "INSERT INTO ILOCKDURATION VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;

	static class Event {
		long id;
		Timestamp time;
		IntrinsicLockState lockState;
	}

	private final Map<Long, Map<Long, Event>> f_threadToLockToEvent = new HashMap<Long, Map<Long, Event>>();

	public IntrinsicLockDurationRowInserter(final Connection c)
			throws SQLException {
		f_ps = c.prepareStatement(f_psQ);
	}

	public void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
			f_threadToLockToEvent.clear();
		}
	}

	private Event getEvent(long inThread, long lock) {
		Map<Long, Event> lockToEvent = f_threadToLockToEvent.get(inThread);
		if (lockToEvent == null) {
			lockToEvent = new HashMap<Long, Event>();
			f_threadToLockToEvent.put(inThread, lockToEvent);
		}
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
		final Event event = getEvent(inThread, lock);
		if (lockState == IntrinsicLockState.BEFORE_ACQUISITION) {
			// event information is saved below
		} else if (lockState == IntrinsicLockState.AFTER_ACQUISITION) {
			if (event.lockState == IntrinsicLockState.BEFORE_ACQUISITION) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.BLOCKING);
			} else {
				FLog.createErrorStatus(event.lockState + " cannot proceed "
						+ lockState + " for lock " + lock + " in thread "
						+ inThread);
			}
		} else if (lockState == IntrinsicLockState.BEFORE_WAIT) {
			if (event.lockState == IntrinsicLockState.AFTER_ACQUISITION) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.HOLDING);
			} else {
				FLog.createErrorStatus(event.lockState + " cannot proceed "
						+ lockState + " for lock " + lock + " in thread "
						+ inThread);
			}
		} else if (lockState == IntrinsicLockState.AFTER_WAIT) {
			if (event.lockState == IntrinsicLockState.BEFORE_WAIT) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.WAITING);
			} else {
				FLog.createErrorStatus(event.lockState + " cannot proceed "
						+ lockState + " for lock " + lock + " in thread "
						+ inThread);
			}
		} else if (lockState == IntrinsicLockState.AFTER_RELEASE) {
			if (event.lockState == IntrinsicLockState.AFTER_ACQUISITION
					|| event.lockState == IntrinsicLockState.AFTER_WAIT) {
				insert(runId, inThread, lock, event.time, event.id, time, id,
						IntrinsicLockDurationState.HOLDING);
			} else {
				FLog.createErrorStatus(event.lockState + " cannot proceed "
						+ lockState + " for lock " + lock + " in thread "
						+ inThread);
			}
		} else {
			FLog.createErrorStatus("Unknown intrinsic lock state " + lockState);
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
			FLog.logError("Insert failed: ILOCKDURATION", e);
		}

	}
}
