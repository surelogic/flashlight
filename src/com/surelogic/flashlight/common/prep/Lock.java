package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.*;
import static com.surelogic._flashlight.common.FlagType.*;
import static com.surelogic._flashlight.common.IdConstants.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public abstract class Lock extends Event {
	static final long FINAL_EVENT = Long.MAX_VALUE;

	private final BeforeTrace before;

	public Lock(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(i);
		this.before = before;
	}

	public void parse(final int runId, final PreppedAttributes attributes)
			throws SQLException {
		long nanoTime = attributes.getLong(TIME);
		long inThread = attributes.getLong(THREAD);
		long inClass = attributes.getLong(IN_CLASS);
		int lineNumber = attributes.getInt(LINE);
		long lock = attributes.getLong(LOCK);
		Boolean lockIsThis = attributes.getBoolean(THIS_LOCK);
		Boolean lockIsClass = attributes.getBoolean(CLASS_LOCK);
		if (getType() == LockType.UTIL) {
			lockIsThis = null;
			lockIsClass = null;
		}
		Boolean success = null;
		if (attributes.getBoolean(GOT_LOCK) || attributes.getBoolean(RELEASED_LOCK)) {
			success = true;
		}
		if ((nanoTime == ILLEGAL_ID) || (inThread == ILLEGAL_ID) || (inClass == ILLEGAL_ID)
				|| (lineNumber == ILLEGAL_LINE) || (lock == ILLEGAL_ID)) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, file, line or lock in "
							+ getXMLElementName());
			return;
		}
		final Timestamp time = getTimestamp(nanoTime);
		before.threadEvent(inThread);
		final long id = f_rowInserter.insertLock(runId, false, time, inThread,
				inClass, lineNumber, lock, getType(), getState(), success,
				lockIsThis, lockIsClass);
		f_rowInserter.event(runId, id, time, inThread, lock, getState(),
				success != Boolean.FALSE);
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		super.setup(c, start, startNS, scanResults);
	}

	@Override
	public final void flush(final int runId, final long endTime)
			throws SQLException {
		f_rowInserter.flush(runId, getTimestamp(endTime));
		f_rowInserter.close();
		super.flush(runId, endTime);
	}

	abstract protected LockState getState();

	abstract protected LockType getType();
}
