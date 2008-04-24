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

public abstract class UtilConcurrentLock extends Event {
	private static final String f_psQ = "INSERT INTO UCLOCK (Run,Id,TS,InThread,InClass,AtLine,Lock,Type,Success) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static long f_id;

	private static PreparedStatement f_ps;

	private final BeforeTrace before;

	public UtilConcurrentLock(BeforeTrace before) {
		this.before = before;
	}

	public void parse(int runId, Attributes attributes) {
		long nanoTime = -1;
		long inThread = -1;
		long inClass = -1;
		int lineNumber = -1;
		long lock = -1;
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
				}
			}
		}
		if (nanoTime == -1 || inThread == -1 || inClass == -1
				|| lineNumber == -1 || lock == -1) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, file, line, or lock in "
							+ getXMLElementName());
			return;
		}
		final long id = f_id++;
		final Timestamp time = getTimestamp(nanoTime);
		before.threadEvent(inThread);
		insert(runId, id, time, inThread, inClass, lineNumber, lock, getType(),
				parseSuccess(attributes));
		useObject(inThread);
		useObject(inClass);
		useObject(lock);
	}

	private void insert(int runId, long id, Timestamp time, long inThread,
			long inClass, int lineNumber, long lock, String type,
			Boolean success) {
		try {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, id);
			f_ps.setTimestamp(3, time);
			f_ps.setLong(4, inThread);
			f_ps.setLong(5, inClass);
			f_ps.setInt(6, lineNumber);
			f_ps.setLong(7, lock);
			f_ps.setString(8, type);
			JDBCUtils.setNullableString(9, f_ps, success == null ? null
					: (success ? "Y" : "N"));
			f_ps.executeUpdate();
		} catch (final SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: UCLOCK", e);
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
		}
	}

	public final void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}

	}

	abstract protected String getType();

	abstract protected Boolean parseSuccess(Attributes attr);
}
