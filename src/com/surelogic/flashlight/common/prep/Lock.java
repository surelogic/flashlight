package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.FlagType.CLASS_LOCK;
import static com.surelogic._flashlight.common.FlagType.GOT_LOCK;
import static com.surelogic._flashlight.common.FlagType.RELEASED_LOCK;
import static com.surelogic._flashlight.common.FlagType.THIS_LOCK;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public abstract class Lock extends Event {
	static final long FINAL_EVENT = Long.MAX_VALUE;

	public Lock(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public void parse(final PreppedAttributes attributes) throws SQLException {
		final long nanoTime = attributes.getEventTime();
		final long inThread = attributes.getThreadId();
		final long trace = attributes.getTraceId();
		final long lock = attributes.getLockId();
		Boolean lockIsThis = attributes.getBoolean(THIS_LOCK);
		Boolean lockIsClass = attributes.getBoolean(CLASS_LOCK);
		if (getType() == LockType.UTIL) {
			lockIsThis = null;
			lockIsClass = null;
		}
		Boolean success = null;
		if (attributes.getBoolean(GOT_LOCK)
				|| attributes.getBoolean(RELEASED_LOCK)) {
			success = true;
		}
		if ((nanoTime == ILLEGAL_ID) || (inThread == ILLEGAL_ID)
				|| (trace == ILLEGAL_ID) || (lock == ILLEGAL_ID)) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, site, or lock in "
							+ getXMLElementName());
			return;
		}
		final Timestamp time = getTimestamp(nanoTime);
		final long id = f_rowInserter.insertLock(false, time, inThread, trace,
				lock, getType(), getState(), success, lockIsThis, lockIsClass);
		f_rowInserter.event(id, time, inThread, trace, lock, getState(),
				success != Boolean.FALSE);
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
	}

	@Override
	public final void flush(final long endTime) throws SQLException {
		f_rowInserter.flush(getTimestamp(endTime));
		f_rowInserter.close();
		super.flush(endTime);
	}

	abstract protected LockState getState();

	abstract protected LockType getType();
}
