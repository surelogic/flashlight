package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.IdConstants.ILLEGAL_ID;

import java.sql.SQLException;
import java.util.logging.Level;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.logging.SLLogger;

abstract class HappensBefore extends Event {

    protected final ClassHierarchy f_hbConfig;

    HappensBefore(ClassHierarchy hbConfig) {
        f_hbConfig = hbConfig;
    }

    @Override
    public void parse(PreppedAttributes attributes) throws SQLException {
        final String id = attributes.getString(AttributeType.ID);
        final long nanoStart = attributes.getLong(AttributeType.NANO_START);
        final long nanoEnd = attributes.getLong(AttributeType.NANO_END);
        final long inThread = attributes.getThreadId();
        final long trace = attributes.getTraceId();
        final long site = attributes.getLong(AttributeType.SITE_ID);
        if (nanoStart == ILLEGAL_ID || nanoEnd == ILLEGAL_ID
                || inThread == ILLEGAL_ID || trace == ILLEGAL_ID
                || site == ILLEGAL_ID) {
            SLLogger.getLogger().log(
                    Level.SEVERE,
                    "Missing nano-time, thread, site, or field in "
                            + getXMLElementName());
            return;
        }
        parseRest(attributes, id, nanoStart, nanoEnd, inThread, trace, site);
    }

    abstract void parseRest(PreppedAttributes attributes, String id,
            long nanoStart, long nanoEnd, long inThread, long trace, long site)
            throws SQLException;

}
