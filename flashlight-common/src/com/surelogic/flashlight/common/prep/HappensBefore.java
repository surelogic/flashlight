package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.DIRECTION;
import static com.surelogic._flashlight.common.AttributeType.TARGET;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_RECEIVER_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class HappensBefore extends Event {

    private int count;
    private PreparedStatement f_ps;

    @Override
    public String getXMLElementName() {
        return "happens-before";
    }

    @Override
    public void parse(PreppedAttributes attributes) throws SQLException {
        final long nanoTime = attributes.getEventTime();
        final long inThread = attributes.getThreadId();
        final long trace = attributes.getTraceId();
        final long target = attributes.getLong(TARGET);
        final boolean isFrom = "from".equals(attributes.getString(DIRECTION));
        if (nanoTime == ILLEGAL_ID || inThread == ILLEGAL_ID
                || trace == ILLEGAL_ID || target == ILLEGAL_RECEIVER_ID) {
            SLLogger.getLogger().log(
                    Level.SEVERE,
                    "Missing nano-time, thread, site, or field in "
                            + getXMLElementName());
            return;
        }
        insert(nanoTime, inThread, trace, target, isFrom);
    }

    private void insert(final long nanoTime, final long inThread,
            final long trace, final long target, final boolean isFrom)
            throws SQLException {
        int idx = 1;
        f_ps.setLong(idx++, inThread);
        f_ps.setLong(idx++, target);
        f_ps.setLong(idx++, trace);
        f_ps.setTimestamp(idx++, getTimestamp(nanoTime), now);
        f_ps.setString(idx++, isFrom ? "Y" : "N");
        if (doInsert) {
            f_ps.addBatch();
            if (++count == 10000) {
                f_ps.executeBatch();
                count = 0;
            }
        }
    }

    @Override
    public void setup(final Connection c, final Timestamp start,
            final long startNS, final ScanRawFilePreScan scanResults)
            throws SQLException {
        super.setup(c, start, startNS, scanResults);
        f_ps = c.prepareStatement("INSERT INTO HAPPENSBEFORE (THREAD,TARGET,TRACE,TS,ID_FROM) VALUES (?,?,?,?,?)");
    }

    @Override
    public void flush(final long endTime) throws SQLException {
        if (count > 0) {
            f_ps.executeBatch();
            count = 0;
        }
        f_ps.close();
        super.flush(endTime);
    }
}
