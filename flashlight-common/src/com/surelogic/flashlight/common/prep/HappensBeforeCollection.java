package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.HappensBeforeConfig.HBType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class HappensBeforeCollection extends HappensBefore {

    private int sourceCount;
    private int targetCount;
    private PreparedStatement f_sourcePs;
    private PreparedStatement f_targetPs;

    public HappensBeforeCollection(ClassHierarchy ch) {
        super(ch);
    }

    @Override
    void parseRest(PreppedAttributes attributes, long nanoTime, long inThread,
            long trace, long site) throws SQLException {
        final long coll = attributes.getLong(AttributeType.COLLECTION);
        final long obj = attributes.getLong(AttributeType.OBJECT);
        if (obj == ILLEGAL_ID || coll == ILLEGAL_ID) {
            SLLogger.getLogger().log(Level.SEVERE,
                    "Missing obj or coll in " + getXMLElementName());
            return;
        }
        HBType type = f_hbConfig.getCollectionHbType(site);
        if (type.isSource()) {
            insert(nanoTime, inThread, trace, obj, true);
        }
        if (type.isTarget()) {
            insert(nanoTime, inThread, trace, obj, false);
        }
    }

    @Override
    public void parse(PreppedAttributes attributes) throws SQLException {

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

    @Override
    public String getXMLElementName() {
        return "happens-before-coll";
    }

}