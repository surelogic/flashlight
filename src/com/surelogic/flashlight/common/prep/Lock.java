package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
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
				if ("nano-time".equals(aName)) {
					nanoTime = Long.parseLong(aValue);
				} else if ("thread".equals(aName)) {
					inThread = Long.parseLong(aValue);
				} else if ("in-class".equals(aName)) {
					inClass = Long.parseLong(aValue);
				} else if ("line".equals(aName)) {
					lineNumber = Integer.parseInt(aValue);
				} else if ("lock".equals(aName)) {
					lock = Long.parseLong(aValue);
				} else if ("lock-is-this".equals(aName)) {
					lockIsThis = true;
				} else if ("lock-is-class".equals(aName)) {
					lockIsClass = true;
				} else if ("released-the-lock".equals(aName)) {
					success = "yes".equals(aValue);
				} else if ("got-the-lock".equals(aName)) {
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
		useObject(inThread);
		useObject(inClass);
		useObject(lock);
		f_rowInserter.event(runId, id, time, inThread, lock, getState(),
				success != Boolean.FALSE);
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults,
			final Set<Long> unreferencedObjects,
			final Set<Long> unreferencedFields) throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
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
