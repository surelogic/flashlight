package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.CLASS_NAME;
import static com.surelogic._flashlight.common.AttributeType.CLASS_TYPE;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.MODIFIER;
import static com.surelogic._flashlight.common.AttributeType.THREAD_NAME;
import static com.surelogic._flashlight.common.AttributeType.TYPE;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.ClassType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

public final class ClassDefinition extends AbstractPrep {

  @Override
  public String getXMLElementName() {
    return "class-definition";
  }

  private static final String f_psQ = "INSERT INTO OBJECT (Id,Type,Flag,Threadname,PackageName,ClassName,ClassCode) VALUES (?, ?, ?, ?, ?, ?, ?)";

  private PreparedStatement f_ps;

  private int count;

  @Override
  public final void parse(final PreppedAttributes attributes) throws SQLException {
    final long id = attributes.getLong(ID);
    long type = attributes.getLong(TYPE);
    final String threadName = attributes.getString(THREAD_NAME);
    String className = attributes.getString(CLASS_NAME);
    if (id == ILLEGAL_ID) {
      SLLogger.getLogger().log(Level.SEVERE, "Missing id in " + getXMLElementName());
      return;
    }
    if (type == ILLEGAL_ID) {
      type = id;
    }
    String packageName = null;
    if (className != null) {
      final int lastDot = className.lastIndexOf('.');
      if (lastDot == -1) {
        packageName = "(default)";
      } else {
        packageName = className.substring(0, lastDot);
        className = className.substring(lastDot + 1);
      }
    }

    if (attributes.containsKey(CLASS_TYPE)) {
      ClassType classType = ClassType.fromXmlName(attributes.getString(CLASS_TYPE));
      // XXX We check to see if class type is included in order to be
      // passive with existing code, this check can come out soon.
      int modifiers = Integer.parseInt(attributes.getString(MODIFIER));
      insert(id, type, threadName, packageName, className, classType.getCode(modifiers));
    } else {
      insert(id, type, threadName, packageName, className, "CL:NA");
    }
  }

  private void insert(final long id, final long type, final String threadName, final String packageName, final String className,
      final String classCode) throws SQLException {
    int idx = 1;
    f_ps.setLong(idx++, id);
    f_ps.setLong(idx++, type);
    f_ps.setString(idx++, "C");
    if (threadName != null) {
      f_ps.setString(idx++, threadName);
    } else {
      f_ps.setNull(idx++, Types.VARCHAR);
    }
    if (className != null) {
      f_ps.setString(idx++, packageName);
      f_ps.setString(idx++, className);
    } else {
      f_ps.setNull(idx++, Types.VARCHAR);
      f_ps.setNull(idx++, Types.VARCHAR);
    }
    f_ps.setString(idx++, classCode);
    f_ps.addBatch();
    if (++count == 10000) {
      f_ps.executeBatch();
      count = 0;
    }
  }

  @Override
  public void setup(final Connection c, final Timestamp start, final long startNS, final ScanRawFilePreScan scanResults)
      throws SQLException {
    super.setup(c, start, startNS, scanResults);
    f_ps = c.prepareStatement(f_psQ);
  }

  @Override
  public void flush(final long endTime) throws SQLException {
    if (count > 0) {
      f_ps.executeBatch();
    }
    count = 0;
    f_ps.close();
  }

  @Override
  public void printStats() {

  }
}
