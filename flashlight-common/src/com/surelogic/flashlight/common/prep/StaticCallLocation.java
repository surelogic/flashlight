package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.AttributeType.FILE;
import static com.surelogic._flashlight.common.AttributeType.ID;
import static com.surelogic._flashlight.common.AttributeType.IN_CLASS;
import static com.surelogic._flashlight.common.AttributeType.LINE;
import static com.surelogic._flashlight.common.AttributeType.LOCATION;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLDESC;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLNAME;
import static com.surelogic._flashlight.common.AttributeType.METHODCALLOWNER;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic._flashlight.common.LongSet;
import com.surelogic._flashlight.common.PreppedAttributes;

public final class StaticCallLocation extends AbstractPrep {
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
        f_ps.setLong(idx++, id);
        f_ps.setInt(idx++, attributes.getInt(LINE));
        f_ps.setLong(idx++, attributes.getLong(IN_CLASS));
        f_ps.setString(idx++, attributes.getString(FILE));
        f_ps.setString(idx++, attributes.getString(LOCATION));
        f_ps.setString(idx++, attributes.getString(METHODCALLOWNER));
        f_ps.setString(idx++, attributes.getString(METHODCALLNAME));
        f_ps.setString(idx++, attributes.getString(METHODCALLDESC));
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
        f_ps = c.prepareStatement("INSERT INTO SITE (Id,AtLine,InClass,InFile,Location, MethodClass, MethodCall, MethodSpec) VALUES (?,?,?,?,?,?,?,?)");
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
