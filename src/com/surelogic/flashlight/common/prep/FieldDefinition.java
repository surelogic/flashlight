package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FIELD;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.TYPE;
import static com.surelogic._flashlight.common.FlagType.IS_FINAL;
import static com.surelogic._flashlight.common.FlagType.IS_STATIC;
import static com.surelogic._flashlight.common.FlagType.IS_VOLATILE;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_FIELD_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class FieldDefinition implements IRangePrep {

	private static final String f_psQ = "INSERT INTO FIELD VALUES (?, ?, ?, ?, ?, ?)";

	private PreparedStatement f_ps;
	private ScanRawFileFieldsPreScan preScan;
	private int count;

	public void flush(final long endTime) throws SQLException {
		if (count > 0) {
			f_ps.executeBatch();
			count = 0;
		}
		f_ps.close();
	}

	public String getXMLElementName() {
		return "field-definition";
	}

	public void parse(final PreppedAttributes attributes) throws SQLException {
		final long id = attributes.getLong(ID);
		final long type = attributes.getLong(TYPE);
		final String field = attributes.getString(FIELD);
		final boolean isStatic = attributes.getBoolean(IS_STATIC);
		final boolean isFinal = attributes.getBoolean(IS_FINAL);
		final boolean isVolatile = attributes.getBoolean(IS_VOLATILE);
		if ((id == ILLEGAL_FIELD_ID) || (type == ILLEGAL_ID) || (field == null)) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id, type, or field in field-definition");
			return;
		}
		if (!isStatic) {
			insert(id, type, field, isFinal, isVolatile);
		}
	}

	private void insert(final long id, final long type, final String field,
			final boolean isFinal, final boolean isVolatile)
			throws SQLException {
		if (preScan.isThreadedField(id)) {
			int idx = 1;
			f_ps.setLong(idx++, id);
			if (field != null) {
				f_ps.setString(idx++, field);
			} else {
				f_ps.setNull(idx++, Types.VARCHAR);
			}
			f_ps.setLong(idx++, type);
			f_ps.setString(idx++, "N");
			f_ps.setString(idx++, isFinal ? "Y" : "N");
			f_ps.setString(idx++, isVolatile ? "Y" : "N");
			f_ps.addBatch();
			if (++count == 10000) {
				f_ps.executeBatch();
				count = 0;
			}
		}
	}

	public void printStats() {
		// TODO Auto-generated method stub

	}

	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFileFieldsPreScan st,
			final long begin, final long end) throws SQLException {
		f_ps = c.prepareStatement(f_psQ);
		preScan = st;
	}

}
