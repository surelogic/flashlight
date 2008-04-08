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

public abstract class FieldAccess extends Event {

	private static final String f_psQ = "INSERT INTO ACCESS VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private static PreparedStatement f_ps;

	private static DataPreScan f_scanResults;

	public void parse(int runId, Attributes attributes) {
		long nanoTime = -1;
		long inThread = -1;
		String file = null;
		int lineNumber = -1;
		long field = -1;
		long receiver = -1;
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
				} else if ("field".equals(aName)) {
					field = Long.parseLong(aValue);
				} else if ("receiver".equals(aName)) {
					receiver = Long.parseLong(aValue);
				}
			}
		}
		if (nanoTime == -1 || inThread == -1 || file == null
				|| lineNumber == -1 || field == -1) {
			SLLogger.getLogger().log(
					Level.SEVERE,
					"Missing nano-time, thread, file, line or field in "
							+ getXMLElementName());
			return;
		}
		if (receiver == -1) {
			if (f_scanResults.isSingleThreadedStaticField(field))
				return;
		} else {
			useObject(receiver);
			if (f_scanResults.isThreadedField(field, receiver))
				return;
		}
		insert(runId, nanoTime, inThread, file, lineNumber, field, receiver);
		useObject(inThread);
		useField(field);
	}

	private void insert(int runId, long nanoTime, long inThread, String file,
			int lineNumber, long field, long receiver) {
		try {
			f_ps.setInt(1, runId);
			f_ps.setTimestamp(2, getTimestamp(nanoTime));
			f_ps.setLong(3, inThread);
			f_ps.setString(4, file);
			f_ps.setInt(5, lineNumber);
			f_ps.setLong(6, field);
			f_ps.setString(7, getRW());
			if (receiver == -1) {
				f_ps.setNull(8, Types.BIGINT);
			} else {
				f_ps.setLong(8, receiver);
			}
			f_ps.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: ACCESS", e);
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
			f_ps = c.prepareStatement(f_psQ);
			f_scanResults = scanResults;
		}
	}

	public final void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
			f_scanResults = null;
		}
	}

	/**
	 * Indicates the type of field access.
	 * 
	 * @return <tt>R</tt> for a field read, or <tt>W</tt> for a field write.
	 */
	abstract protected String getRW();
}
