package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.PARENT_ID;
import static com.surelogic._flashlight.common.AttributeType.SITE_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;
import com.surelogic._flashlight.common.PreppedAttributes;

public final class TraceNode extends AbstractPrep {

  public static final LongSet refdSites = new LongScatterSet();

  private PreparedStatement f_ps;
  private int count;

  @Override
  public String getXMLElementName() {
    return "trace-node";
  }

  @Override
  public void parse(final PreppedAttributes attributes) throws SQLException {
    int idx = 1;
    final long id = attributes.getTraceId();
    long parent = attributes.getLong(PARENT_ID);
    if (parent == 0) {
      parent = id;
    }
    final long site = attributes.getLong(SITE_ID);
    if (StaticCallLocation.checkSites) {
      refdSites.add(site);
    }
    f_ps.setLong(idx++, id);
    f_ps.setLong(idx++, site);
    f_ps.setLong(idx++, parent);
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
    f_ps = c.prepareStatement("INSERT INTO TRACE (Id,Site,Parent) VALUES (?,?,?)");
  }

  @Override
  public void flush(final long endTime) throws SQLException {
    if (count > 0) {
      f_ps.executeBatch();
      count = 0;
    }
    super.flush(endTime);
    f_ps.close();
  }

}
