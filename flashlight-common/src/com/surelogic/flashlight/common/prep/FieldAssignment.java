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

    @Override
    public void flush(final long endTime) throws SQLException {
        if (count > 0) {
            f_ps.executeBatch();
            count = 0;
        }
        f_ps.close();
    }

    @Override
    public void printStats() {
        System.out.println(getClass().getName() + " Skipped   = " + skipped);
        System.out.println(getClass().getName() + " Inserted  = " + inserted);
        System.out.println(getClass().getName() + " %Inserted = " + inserted
                * 100.0 / (skipped + inserted));
    }

    @Override
    public void setup(final Connection c, final Timestamp start,
            final long startNS, final ScanRawFileFieldsPreScan scanResults,
            final long begin, final long end) throws SQLException {
        super.setup(c, start, startNS, scanResults, begin, end);
        f_ps = c.prepareStatement(f_psQ);
    }

    @Override
    public String getXMLElementName() {
        return "field-assignment";
    }

    @Override
    public void parse(final PreppedAttributes attributes) throws SQLException {
        final long field = attributes.getLong(AttributeType.FIELD);
        final long value = attributes.getLong(AttributeType.VALUE);
        final Long receiver = attributes.containsKey(AttributeType.RECEIVER) ? attributes
                .getLong(AttributeType.RECEIVER) : null;
        if (receiver == null) {
            return;
        }
        if (field == ILLEGAL_FIELD_ID || receiver == ILLEGAL_RECEIVER_ID) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Missing field or receiver in field-assignment");
        }
        if (receiver == ILLEGAL_RECEIVER_ID || receiver < f_begin
                || receiver > f_end) {
            return;
        }
        if (!f_scanResults.couldBeReferencedObject(receiver)
                || f_scanResults.isSynthetic(field)) {
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
