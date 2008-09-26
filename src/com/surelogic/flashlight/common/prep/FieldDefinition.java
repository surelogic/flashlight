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

import static com.surelogic._flashlight.common.AttributeType.*;
import static com.surelogic._flashlight.common.IdConstants.*;
import static com.surelogic._flashlight.common.FlagType.*;

public final class FieldDefinition extends TrackUnreferenced {

	private static final String f_psQ = "INSERT INTO FIELD VALUES (?, ?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;

	public String getXMLElementName() {
		return "field-definition";
	}

	public void parse(final int runId, final Attributes attributes)
			throws SQLException {
		long id = ILLEGAL_FIELD_ID;
		long type = -1;
		String field = null;
		boolean isStatic = false;
		boolean isFinal = false;
		boolean isVolatile = false;
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				if (ID.matches(aName)) {
					id = Long.parseLong(aValue);
				} else if (TYPE.matches(aName)) {
					type = Long.parseLong(aValue);
				} else if (FIELD.matches(aName)) {
					field = aValue;
				} else if (IS_STATIC.matches(aName)) {
					isStatic = true;
				} else if (IS_FINAL.matches(aName)) {
					isFinal = true;
				} else if (IS_VOLATILE.matches(aName)) {
					isVolatile = true;
				}
			}
		}
		if ((id == ILLEGAL_FIELD_ID) || (type == -1) || (field == null)) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id, type, or field in field-definition");
			return;
		}
		insert(runId, id, type, field, isStatic, isFinal, isVolatile);
		newField(id);
		useObject(type);
	}

	private void insert(final int runId, final long id, final long type,
			final String field, final boolean isStatic, final boolean isFinal,
			final boolean isVolatile) throws SQLException {
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
	}

	@Override
	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults,
			final Set<Long> unreferencedObjects,
			final Set<Long> unreferencedFields) throws SQLException {
		super.setup(c, start, startNS, scanResults, unreferencedObjects,
				unreferencedFields);
		f_ps = c.prepareStatement(f_psQ);
	}

	@Override
	public void flush(final int runId, final long endTime) throws SQLException {
		f_ps.close();
		super.flush(runId, endTime);
	}
}
