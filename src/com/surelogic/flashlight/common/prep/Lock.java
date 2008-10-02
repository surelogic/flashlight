package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.IN_CLASS;
import static com.surelogic._flashlight.common.AttributeType.LINE;
import static com.surelogic._flashlight.common.AttributeType.LOCK;
import static com.surelogic._flashlight.common.AttributeType.THREAD;
import static com.surelogic._flashlight.common.AttributeType.TIME;
import static com.surelogic._flashlight.common.FlagType.CLASS_LOCK;
import static com.surelogic._flashlight.common.FlagType.GOT_LOCK;
import static com.surelogic._flashlight.common.FlagType.RELEASED_LOCK;
import static com.surelogic._flashlight.common.FlagType.THIS_LOCK;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public abstract class Lock extends Event {
	static final long FINAL_EVENT = Long.MAX_VALUE;

	private final BeforeTrace before;

	public Lock(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(i);
		this.before = before;
	}

	public void parse(final int runId, final Attributes attributes)
			throws SQLException {
		long nanoTime = -1;
		long inThread = -1;
		long inClass = -1;
		int lineNumber = -1;
		long lock = -1;
		Boolean lockIsThis = false;
		Boolean lockIsClass = false;
		if (getType() == LockType.UTIL) {
			lockIsThis = null;
			lockIsClass = null;
		}
		Boolean success = null;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if (TIME.matches(aName)) {
					nanoTime = Long.parseLong(aValue);
				} else if (THREAD.matches(aName)) {
					inThread = Long.parseLong(aValue);
				} else if (IN_CLASS.matches(aName)) {
					inClass = Long.parseLong(aValue);
				} else if (LINE.matches(aName)) {
					lineNumber = Integer.parseInt(aValue);
				} else if (LOCK.matches(aName)) {
					lock = Long.parseLong(aValue);
				} else if (THIS_LOCK.matches(aName)) {
					lockIsThis = true;
				} else if (CLASS_LOCK.matches(aName)) {
					lockIsClass = true;
				} else if (RELEASED_LOCK.matches(aName)) {
					success = "yes".equals(aValue);
				} else if (GOT_LOCK.matches(aName)) {
					success = "yes".equals(aValue);
				}
			}
		}
		if ((nanoTime == -1) || (inThread == -1) || (inClass == -1)
				|| (lineNumber == -1) || (lock == -1)) {
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
