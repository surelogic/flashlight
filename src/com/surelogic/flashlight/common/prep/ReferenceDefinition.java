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

public abstract class ReferenceDefinition extends TrackUnreferenced {

	private static final String f_psQ = "INSERT INTO OBJECT VALUES (?, ?, ?, ?, ?, ?)";

	private static PreparedStatement f_ps;

	public final void parse(final int runId, final Attributes attributes) {
		long id = -1;
		long type = -1;
		String threadName = null;
		String className = null;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if ("id".equals(aName)) {
					id = Long.parseLong(aValue);
				} else if ("type".equals(aName)) {
					type = Long.parseLong(aValue);
				} else if ("thread-name".equals(aName)) {
					threadName = aValue;
				} else if ("class-name".equals(aName)) {
					className = aValue;
				}
			}
		}
		if (id == -1) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id in " + getXMLElementName());
			return;
		}
		newObject(id);
		if (type == -1) {
			type = id;
		} else {
			useObject(type);
		}
		String packageName = null;
		if (className != null) {
			int lastDot = className.lastIndexOf('.');
			if (lastDot == -1) {
				packageName = "(default)";
			} else {
				packageName = className.substring(0, lastDot);
				className = className.substring(lastDot + 1);
			}
		}
		insert(runId, id, type, threadName, packageName, className);
	}

	private void insert(int runId, long id, long type, String threadName,
			String packageName, String className) {
		try {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, id);
			f_ps.setLong(3, type);
			if (threadName != null) {
				f_ps.setString(4, threadName);
			} else {
				f_ps.setNull(4, Types.VARCHAR);
			}
			if (className != null) {
				f_ps.setString(5, packageName);
				f_ps.setString(6, className);
			} else {
				f_ps.setNull(5, Types.VARCHAR);
				f_ps.setNull(6, Types.VARCHAR);
			}
			f_ps.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: OBJECT", e);
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
		}
	}

	public final void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
	}
}
