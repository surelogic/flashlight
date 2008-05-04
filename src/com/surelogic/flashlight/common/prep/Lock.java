package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.jdbc.JDBCUtils;
import com.surelogic.common.logging.SLLogger;

public abstract class Lock extends Event {
  static final long FINAL_EVENT = Long.MAX_VALUE;  
  
	private static final String f_psQ = "INSERT INTO LOCK (Run,Id,TS,InThread,InClass,AtLine,Lock,Type,State,Success,LockIsThis,LockIsClass) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";

	private static long f_id;

	private static PreparedStatement f_ps;

	private final BeforeTrace before;

	public Lock(BeforeTrace before) {
		this.before = before;
	}

	public void parse(int runId, Attributes attributes) {
		long nanoTime = -1;
		long inThread = -1;
		long inClass = -1;
		int lineNumber = -1;
		long lock = -1;
		Boolean lockIsThis = false;
		Boolean lockIsClass = false;
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
					lockIsThis = null;
					lockIsClass = null;
				} else if ("got-the-lock".equals(aName)) {
					success = "yes".equals(aValue);
					lockIsThis = null;
					lockIsClass = null;
				}
			}
		}
		if (nanoTime == -1 || inThread == -1 || inClass == -1
				|| lineNumber == -1 || lock == -1) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, file, line or lock in "
							+ getXMLElementName());
			return;
		}
		final long id = f_id++;
		final Timestamp time = getTimestamp(nanoTime);
		before.threadEvent(inThread);
		insert(runId, id, time, inThread, inClass, lineNumber, lock, 
		       getType(), getState(), success, lockIsThis, lockIsClass);
		useObject(inThread);
		useObject(inClass);
		useObject(lock);
		f_rowInserter.event(runId, id, time, inThread, lock, getState(),
				success != Boolean.FALSE);
	}

	// Only called here and from IntrinsicLockDurationInserter
  static void insert(int runId, long id, Timestamp time, long inThread,
			long inClass, int lineNumber, long lock, LockType lockType, LockState lockState, 
			Boolean success, Boolean lockIsThis, Boolean lockIsClass) {
		try {
			int idx = 1;
			f_ps.setInt(idx++, runId);
			f_ps.setLong(idx++, id);
			f_ps.setTimestamp(idx++, time);
			f_ps.setLong(idx++, inThread);
			f_ps.setLong(idx++, inClass);
			f_ps.setInt(idx++, lineNumber);
			f_ps.setLong(idx++, lock);
			f_ps.setString(idx++, lockType.getFlag());
			f_ps.setString(idx++, lockState.toString().replace('_', ' '));
			JDBCUtils.setNullableBoolean(idx++, f_ps, success);
			JDBCUtils.setNullableBoolean(idx++, f_ps, lockIsThis);
			JDBCUtils.setNullableBoolean(idx++, f_ps, lockIsClass);
			f_ps.executeUpdate();
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: ILOCK", e);
		}
	}

	@Override
	public final void setup(final Connection c, final Timestamp start,
			final long startNS, final DataPreScan scanResults,
			Set<Long> unreferencedObjects, Set<Long> unreferencedFields)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
		if (f_ps == null) {
			f_id = 0;
			f_ps = c.prepareStatement(f_psQ);
			f_rowInserter = new IntrinsicLockDurationRowInserter(c);
		}
	}

	public final void flush(final int runId, final long endTime) throws SQLException {
		f_rowInserter.flush(runId, getTimestamp(endTime));
	}

	public final void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
		if (f_rowInserter != null) {
			f_rowInserter.close();
			f_rowInserter = null;
		}
	}

	abstract protected LockState getState();

	abstract protected LockType getType();
}
