package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FIELD;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.MODIFIER;
import static com.surelogic._flashlight.common.AttributeType.TYPE;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_FIELD_ID;
import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.recommend.Visibility;

public class FieldDefinition extends AbstractPrep {

  private static final int SYNTHETIC = 0x00001000;

  private static final String f_psQ = "INSERT INTO FIELD VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  private PreparedStatement f_ps;
  private int count;

  @Override
  public void flush(final long endTime) throws SQLException {
    if (count > 0) {
      f_ps.executeBatch();
      count = 0;
    }
    f_ps.close();
  }

  @Override
  public String getXMLElementName() {
    return "field-definition";
  }

  @Override
  public void parse(final PreppedAttributes attributes) throws SQLException {
    final long id = attributes.getLong(ID);
    final long type = attributes.getLong(TYPE);
    final String field = attributes.getString(FIELD);
    final int mod = attributes.getInt(MODIFIER);
    if (id == ILLEGAL_FIELD_ID || type == ILLEGAL_ID || field == null) {
      SLLogger.getLogger().log(Level.SEVERE, "Missing id, type, or field in field-definition");
      return;
    }
    final boolean isStatic = Modifier.isStatic(mod);
    final boolean isFinal = Modifier.isFinal(mod);
    final boolean isVolatile = Modifier.isVolatile(mod);
    final int visibility = Visibility.toFlag(mod);
    StringBuilder code = new StringBuilder(11);
    code.append("@FL:");
    if (Modifier.isPublic(mod)) {
      code.append("PU");
    } else if (Modifier.isProtected(mod)) {
      code.append("PO");
    } else if (Modifier.isPrivate(mod)) {
      code.append("PR");
    } else {
      code.append("DE");
    }
    code.append(":");
    if (isStatic) {
      code.append("S");
    }
    if (isFinal) {
      code.append("F");
    }
    if (isVolatile) {
      code.append("V");
    }
    if ((mod & SYNTHETIC) != 0) {
      code.append("I");
    }
    if ((mod & SYNTHETIC) == 0 || !isStatic) {// FIXME
      insert(id, type, field, isStatic, isFinal, isVolatile, visibility, code.toString());
    }
  }

  private void insert(final long id, final long type, final String field, final boolean isStatic, final boolean isFinal,
      final boolean isVolatile, final int visibility, final String code) throws SQLException {
    int idx = 1;
    f_ps.setLong(idx++, id);
    if (field != null) {
      f_ps.setString(idx++, field);
    } else {
      f_ps.setNull(idx++, Types.VARCHAR);
    }
    f_ps.setLong(idx++, type);
    f_ps.setInt(idx++, visibility);
    f_ps.setString(idx++, isStatic ? "Y" : "N");
    f_ps.setString(idx++, isFinal ? "Y" : "N");
    f_ps.setString(idx++, isVolatile ? "Y" : "N");
    f_ps.setString(idx++, code);
    f_ps.addBatch();
    if (++count == 10000) {
      f_ps.executeBatch();
      count = 0;
    }
  }

  @Override
  public void printStats() {
    // Do nothing
  }

  @Override
  public void setup(final Connection c, final Timestamp start, final long startNS, final ScanRawFilePreScan scanResults)
      throws SQLException {
    super.setup(c, start, startNS, scanResults);
    f_ps = c.prepareStatement(f_psQ);
  }

}
