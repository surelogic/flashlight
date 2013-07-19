package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FILE;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.IN_CLASS;
import static com.surelogic._flashlight.common.AttributeType.LINE;
import static com.surelogic._flashlight.common.AttributeType.LOCATION;
import static com.surelogic._flashlight.common.AttributeType.LOCATIONMOD;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLDESC;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLMOD;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLNAME;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLOWNER;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic._flashlight.common.LongSet;
import com.surelogic._flashlight.common.PreppedAttributes;

public final class StaticCallLocation extends AbstractPrep {

    private static final int SYNTHETIC = 0x00001000;

    public static final boolean checkSites = false;
    public static LongSet validSites = new LongSet();

    private PreparedStatement f_ps;
    private int count;

    @Override
    public String getXMLElementName() {
        return "static-call-location";
    }

    @Override
    public void parse(final PreppedAttributes attributes) throws SQLException {
        int idx = 1;
        final long id = attributes.getLong(ID);
        if (checkSites) {
            validSites.add(id);
        }
        String loc = attributes.getString(LOCATION);
        String callName = attributes.getString(METHODCALLNAME);
        f_ps.setLong(idx++, id);
        f_ps.setInt(idx++, attributes.getInt(LINE));
        f_ps.setLong(idx++, attributes.getLong(IN_CLASS));
        f_ps.setString(idx++, attributes.getString(FILE));
        f_ps.setString(idx++, loc);
        f_ps.setString(idx++,
                getMethodCode(loc, attributes.getInt(LOCATIONMOD)));
        f_ps.setString(idx++, attributes.getString(METHODCALLOWNER));
        f_ps.setString(idx++, callName);
        f_ps.setString(idx++, attributes.getString(METHODCALLDESC));
        f_ps.setString(
                idx++,
                callName == null ? null : getMethodCode(callName,
                        attributes.getInt(METHODCALLMOD)));
        if (doInsert) {
            f_ps.addBatch();
            if (++count == 10000) {
                f_ps.executeBatch();
                count = 0;
            }
        }
    }

    private static String getMethodCode(String name, int mod) {
        StringBuilder code = new StringBuilder(11);
        code.append('@');
        if (name.equals("<init>")) {
            code.append("CO");
        } else if (name.equals("<clinit>")) {
            code.append("IT");
        } else {
            code.append("ME");
        }
        code.append(':');
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
        if (Modifier.isStatic(mod)) {
            code.append("S");
        }
        if (Modifier.isFinal(mod)) {
            code.append("F");
        }
        if ((mod & SYNTHETIC) != 0) {
            code.append("I");
        }
        if (Modifier.isAbstract(mod)) {
            code.append("A");
        }
        return code.toString();
    }

    @Override
    public void setup(final Connection c, final Timestamp start,
            final long startNS, final ScanRawFilePreScan scanResults)
            throws SQLException {
        super.setup(c, start, startNS, scanResults);
        f_ps = c.prepareStatement("INSERT INTO SITE (Id,AtLine,InClass,InFile,Location,LocationCode,MethodClass,MethodCall,MethodSpec,MethodCode) VALUES (?,?,?,?,?,?,?,?,?,?)");
    }

    @Override
    public void flush(final long endTime) throws SQLException {
        if (count > 0) {
            f_ps.executeBatch();
        }
        count = 0;
        super.flush(endTime);
        f_ps.close();
    }

}
