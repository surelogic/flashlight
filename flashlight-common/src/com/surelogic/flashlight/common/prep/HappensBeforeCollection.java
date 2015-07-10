package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeRule;
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
  void parseRest(PreppedAttributes attributes, String id, long nanoStart, long nanoEnd, long inThread, long trace, long site)
      throws SQLException {
    final long coll = attributes.getLong(AttributeType.COLLECTION);
    final long obj = attributes.getLong(AttributeType.OBJECT);
    if (obj == ILLEGAL_ID || coll == ILLEGAL_ID) {
      SLLogger.getLogger().log(Level.SEVERE, "Missing obj or coll in " + getXMLElementName());
      return;
    }
    HappensBeforeRule rule = f_hbConfig.getHBRule(id, site);
    if (rule.getType().isSource()) {
      if (rule.isCallIn()) {
        insert(id, nanoEnd, inThread, trace, coll, obj, true);
      } else {
        insert(id, nanoStart, inThread, trace, coll, obj, true);
      }
    }
    if (rule.getType().isTarget()) {
      if (rule.isCallIn()) {
        insert(id, nanoStart, inThread, trace, coll, obj, false);
      } else {
        insert(id, nanoEnd, inThread, trace, coll, obj, false);
      }
    }
  }

  @SuppressWarnings("resource")
  private void insert(final String id, final long nanoTime, final long inThread, final long trace, final long coll, final long obj,
      final boolean isSource) throws SQLException {
    PreparedStatement ps = isSource ? f_sourcePs : f_targetPs;
    int idx = 1;
    ps.setString(idx++, id);
    ps.setLong(idx++, coll);
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
  public void setup(final Connection c, final Timestamp start, final long startNS, final ScanRawFilePreScan scanResults)
      throws SQLException {
    super.setup(c, start, startNS, scanResults);
    f_sourcePs = c.prepareStatement("INSERT INTO HAPPENSBEFORECOLLSOURCE (ID,COLL,OBJ,TS,INTHREAD,TRACE) VALUES (?,?,?,?,?,?)");
    f_targetPs = c.prepareStatement("INSERT INTO HAPPENSBEFORECOLLTARGET (ID,COLL,OBJ,TS,INTHREAD,TRACE) VALUES (?,?,?,?,?,?)");
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
