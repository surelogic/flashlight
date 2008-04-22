package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public abstract class IntrinsicLock extends Event {

	private static final String f_psQ = "INSERT INTO ILOCK VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static long f_id;

	private static PreparedStatement f_ps;

	private static IntrinsicLockDurationRowInserter f_rowInserter;

	private final BeforeTrace before;

	public IntrinsicLock(BeforeTrace before) {
		this.before = before;
	}

	public void parse(int runId, Attributes attributes) {
		long nanoTime = -1;
		long inThread = -1;
		String file = null;
		int lineNumber = -1;
		long lock = -1;
		boolean lockIsThis = false;
		boolean lockIsClass = false;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if ("nano-time".equals(aName)) {
					nanoTime = Long.parseLong(aValue);
				} else if ("thread".equals(aName)) {
					inThread = Long.parseLong(aValue);
				} else if ("file".equals(aName)) {
					file = aValue;
				} else if ("line".equals(aName)) {
					lineNumber = Integer.parseInt(aValue);
				} else if ("lock".equals(aName)) {
					lock = Long.parseLong(aValue);
				} else if ("lock-is-this".equals(aName)) {
					lockIsThis = true;
				} else if ("lock-is-class".equals(aName)) {
					lockIsClass = true;
				}
			}
		}
		if (nanoTime == -1 || inThread == -1 || file == null
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
		insert(runId, id, time, inThread, file, lineNumber, lock, getState(),
				lockIsThis, lockIsClass);
		useObject(inThread);
		useObject(lock);
		f_rowInserter.event(runId, id, time, inThread, lock, getState());
	}

	private void insert(int runId, long id, Timestamp time, long inThread,
			String file, int lineNumber, long lock,
			IntrinsicLockState lockState, boolean lockIsThis,
			boolean lockIsClass) {
		try {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, id);
			f_ps.setTimestamp(3, time);
			f_ps.setLong(4, inThread);
			f_ps.setString(5, file);
			f_ps.setInt(6, lineNumber);
			f_ps.setLong(7, lock);
			f_ps.setString(8, lockState.toString().replace('_', ' '));
			if (lockIsThis) {
				f_ps.setString(9, "Y");
			} else {
				f_ps.setNull(9, Types.CHAR);
			}
			if (lockIsClass) {
				f_ps.setString(10, "Y");
			} else {
				f_ps.setNull(10, Types.CHAR);
			}
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

	public final void flush(final int runId) throws SQLException {
		f_rowInserter.flush(runId);
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

	abstract protected IntrinsicLockState getState();
}
