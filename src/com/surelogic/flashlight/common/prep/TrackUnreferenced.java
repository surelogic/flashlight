package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic.common.logging.SLLogger;

public abstract class TrackUnreferenced implements IPrep {
	protected static final Logger LOG = SLLogger
			.getLoggerFor(TrackUnreferenced.class);

	private final Map<String, String> attrs = new HashMap<String, String>();

	protected void parseAttrs(final Attributes attributes) {
		attrs.clear();
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				attrs.put(aName, aValue);
				if (aValue == null) {
					LOG.log(Level.SEVERE, "Null for " + aName, new Throwable());
				}
			}
		}
	}

	protected String getAttr(final AttributeType t) {
		final String val = attrs.get(t.label()); // FIX
		if (val == null) {
			LOG.log(Level.SEVERE, "Null for " + t, new Throwable());
		}
		return val;
	}

	public void setup(final Connection c, final Timestamp start,
			final long startNS, final ScanRawFilePreScan scanResults)
			throws SQLException {
		// Nothing to do
	}

	public void flush(final int runId, final long endTime) throws SQLException {
		// Nothing to do
	}

	public void printStats() {
		// Nothing to do
	}
}
