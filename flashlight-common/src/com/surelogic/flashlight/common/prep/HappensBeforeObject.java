package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class HappensBeforeObject extends Event {

    private int sourceCount;
    private int targetCount;
    private PreparedStatement f_sourcePs;
    private PreparedStatement f_targetPs;

    @Override
    public String getXMLElementName() {
        return "happens-before-obj";
    }

    @Override
    public void parse(PreppedAttributes attributes) throws SQLException {
        final long nanoTime = attributes.getEventTime();
        final long inThread = attributes.getThreadId();
        final long trace = attributes.getTraceId();
        final long obj = attributes.getLong(AttributeType.OBJECT);
        final boolean isSource = attributes.getBoolean(AttributeType.ISSOURCE);
        if (nanoTime == ILLEGAL_ID || inThread == ILLEGAL_ID
                || trace == ILLEGAL_ID || obj == ILLEGAL_ID) {
            SLLogger.getLogger().log(
                    Level.SEVERE,
                    "Missing nano-time, thread, site, or field in "
                            + getXMLElementName());
            return;
        }
        insert(nanoTime, inThread, trace, obj, isSource);
    }

    private void insert(final long nanoTime, final long inThread,
            final long trace, final long obj, final boolean isSource)
            throws SQLException {
        PreparedStatement ps = isSource ? f_sourcePs : f_targetPs;
        int idx = 1;
        ps.setLong(idx++, obj);
        ps.setTimestamp(idx++, getTimestamp(nanoTime), now);
        ps.setLong(idx++, inThread);
        ps.setLong(idx++, trace);
        if (doInsert) {
            ps.addBatch();
            if (isSource) {
                if (++sourceCount == 10000) {
                    f_sourcePs.executeBatch();
                    sourceCount = 0;
                }
            } else {
                if (++targetCount == 10000) {
                    f_targetPs.executeBatch();
                    targetCount = 0;
                }
            }

        }
    }

    @Override
    public void setup(final Connection c, final Timestamp start,
            final long startNS, final ScanRawFilePreScan scanResults)
            throws SQLException {
        super.setup(c, start, startNS, scanResults);
        f_sourcePs = c
                .prepareStatement("INSERT INTO HAPPENSBEFORESOURCE (OBJ,TS,INTHREAD,TRACE) VALUES (?,?,?,?)");
        f_targetPs = c
                .prepareStatement("INSERT INTO HAPPENSBEFORETARGET (OBJ,TS,INTHREAD,TRACE) VALUES (?,?,?,?)");
    }

    @Override
    public void flush(final long endTime) throws SQLException {
        if (sourceCount > 0) {
            f_sourcePs.executeBatch();
            sourceCount = 0;
        }
        if (targetCount > 0) {
            f_targetPs.executeBatch();
            targetCount = 0;
        }
        f_sourcePs.close();
        f_targetPs.close();
        super.flush(endTime);
    }
}
