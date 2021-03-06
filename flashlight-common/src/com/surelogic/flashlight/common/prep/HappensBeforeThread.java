package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.TOTHREAD;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeRule;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public class HappensBeforeThread extends HappensBefore {

  public HappensBeforeThread(ClassHierarchy hbConfig) {
    super(hbConfig);
  }

  private int count;
  private PreparedStatement f_ps;

  @Override
  public String getXMLElementName() {
    return "happens-before-thread";
  }

  @Override
  void parseRest(PreppedAttributes attributes, String id, long nanoStart, long nanoEnd, long inThread, long trace, long site)
      throws SQLException {
    final long toThread = attributes.getLong(TOTHREAD);
    if (toThread == ILLEGAL_ID) {
      SLLogger.getLogger().log(Level.SEVERE, "Missing to-Thread in " + getXMLElementName());
      return;
    }
    HappensBeforeRule rule = f_hbConfig.getHBRule(id, site);
    if (rule.getType().isFrom()) {
      if (rule.isCallIn()) {
        insert(id, nanoEnd, inThread, trace, inThread, toThread);
      } else {
        insert(id, nanoStart, inThread, trace, inThread, toThread);
      }
    }
    if (rule.getType().isTo()) {
      if (rule.isCallIn()) {
        insert(id, nanoStart, inThread, trace, toThread, inThread);
      } else {
        insert(id, nanoEnd, inThread, trace, toThread, inThread);
      }
    }
  }

  private void insert(final String id, final long nanoTime, final long inThread, final long trace, final long source,
      final long target) throws SQLException {
    int idx = 1;
    f_ps.setString(idx++, id);
    f_ps.setLong(idx++, source);
    f_ps.setLong(idx++, target);
    f_ps.setTimestamp(idx++, getTimestamp(nanoTime), now);
    f_ps.setLong(idx++, inThread);
    f_ps.setLong(idx++, trace);
    if (doInsert) {
      f_ps.addBatch();
      if (++count == 10000) {
        f_ps.executeBatch();
        count = 0;
      }
    }
  }

  @Override
  public void setup(final Connection c, final Timestamp start, final long startNS, final ScanRawFilePreScan scanResults)
      throws SQLException {
    super.setup(c, start, startNS, scanResults);
    f_ps = c.prepareStatement("INSERT INTO HAPPENSBEFORE (ID, SOURCE,TARGET,TS,INTHREAD,TRACE) VALUES (?,?,?,?,?,?)");
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
