package com.surelogic.flashlight.jobs.prep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public final class FieldDefinition extends TrackUnreferenced {

	private static final String f_psQ = "INSERT INTO FIELD VALUES (?, ?, ?, ?, ?, ?, ?)";

	private static PreparedStatement f_ps;

	public String getXMLElementName() {
		return "field-definition";
	}

	public void parse(final int runId, final Attributes attributes) {
		long id = -1;
		long type = -1;
		String field = null;
		boolean isStatic = false;
		boolean isFinal = false;
		boolean isVolatile = false;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if ("id".equals(aName)) {
					id = Long.parseLong(aValue);
				} else if ("type".equals(aName)) {
					type = Long.parseLong(aValue);
				} else if ("field".equals(aName)) {
					field = aValue;
				} else if ("static".equals(aName)) {
					isStatic = true;
				} else if ("final".equals(aName)) {
					isFinal = true;
				} else if ("volatile".equals(aName)) {
					isVolatile = true;
				}
			}
		}
		if (id == -1 || type == -1 || field == null) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id, type, or field in field-definition");
			return;
		}
		insert(runId, id, type, field, isStatic, isFinal, isVolatile);
		newField(id);
		useObject(type);
	}

	private void insert(int runId, long id, long type, String field,
			boolean isStatic, boolean isFinal, boolean isVolatile) {
		try {
			f_ps.setInt(1, runId);
			f_ps.setLong(2, id);
			if (field != null) {
				f_ps.setString(3, field);
			} else {
				f_ps.setNull(3, Types.VARCHAR);
			}
			f_ps.setLong(4, type);
			f_ps.setString(5, isStatic ? "Y" : "N");
			f_ps.setString(6, isFinal ? "Y" : "N");
			f_ps.setString(7, isVolatile ? "Y" : "N");
			f_ps.executeUpdate();
		} catch (SQLException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Insert failed: FIELD", e);
		}
	}

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final DataPreScan scanResults,
			Set<Long> unreferencedObjects, Set<Long> unreferencedFields)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
		if (f_ps == null) {
			f_ps = c.prepareStatement(f_psQ);
		}
	}

	public void close() throws SQLException {
		if (f_ps != null) {
			f_ps.close();
			f_ps = null;
		}
	}
}
