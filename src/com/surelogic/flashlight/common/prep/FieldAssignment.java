package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_FIELD_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_RECEIVER_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class FieldAssignment extends RangedEvent {
	private static final String f_psQ = "INSERT INTO FIELDASSIGNMENT VALUES (?, ?, ?)";

	private PreparedStatement f_ps;
	private int count;

	private long skipped, inserted;

	public void flush(final long endTime) throws SQLException {
		if (count > 0) {
			f_ps.executeBatch();
			count = 0;
		}
		f_ps.close();
	}

	public void printStats() {
		System.out.println(getClass().getName() + " Skipped   = " + skipped);
		System.out.println(getClass().getName() + " Inserted  = " + inserted);
		System.out.println(getClass().getName() + " %Inserted = "
				+ (inserted * 100.0 / (skipped + inserted)));
	}

	@Override
	public void setup(Connection c, Timestamp start, long startNS,
			ScanRawFileFieldsPreScan scanResults, long begin, long end)
			throws SQLException {
		super.setup(c, start, startNS, scanResults, begin, end);
		f_ps = c.prepareStatement(f_psQ);
	}

	public String getXMLElementName() {
		return "field-assignment";
	}

	public void parse(PreppedAttributes attributes) throws SQLException {
		long field = attributes.getLong(AttributeType.FIELD);
		long value = attributes.getLong(AttributeType.VALUE);
		Long receiver = attributes.containsKey(AttributeType.RECEIVER) ? attributes
				.getLong(AttributeType.RECEIVER)
				: null;
		if (field == ILLEGAL_FIELD_ID
				|| (receiver != null && receiver == ILLEGAL_RECEIVER_ID)) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Missing id, type, or field in field-definition");
		}
		if (value == ILLEGAL_RECEIVER_ID || value < f_begin || value > f_end) {
			return;
		}
		// FIXME We check to see if the object could be referenced.
		// Unfortunately, we can't check both field and receiver, and so we are
		// going to put some entries into the database that have invalid
		// receivers. Because of this, we won't have a foreign key constraint on
		// receiver in add_constraints.sql
		if (!f_scanResults.couldBeReferencedObject(value)) {
			skipped++;
			return;
		}
		inserted++;
		insert(field, value, receiver);
	}

	private void insert(final long field, final long value, final Long receiver)
			throws SQLException {
		int idx = 1;
		f_ps.setLong(idx++, field);
		f_ps.setLong(idx++, value);
		if (receiver == null) {
			f_ps.setNull(idx++, Types.BIGINT);
		} else {
			f_ps.setLong(idx++, receiver);
		}
		f_ps.addBatch();
		if (++count == 10000) {
			f_ps.executeBatch();
			count = 0;
		}
	}

}
