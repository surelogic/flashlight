package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.xml.sax.Attributes;

import com.surelogic.common.logging.SLLogger;

public abstract class TrackUnreferenced implements IPrep {
	protected static final Logger LOG = SLLogger
			.getLoggerFor(TrackUnreferenced.class);

	protected static final String LINE = "line";
	protected static final String THREAD = "thread";
	protected static final String NANO_TIME = "nano-time";
	protected static final String CLASS = "in-class";

	private final Map<String, String> attrs = new HashMap<String, String>();

	protected void parseAttrs(Attributes attributes) {
		attrs.clear();
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				attrs.put(aName, aValue);
				if (aValue == null) {
					LOG.severe("Null for " + aName);
				}
			}
		}
	}

	protected String getAttr(String name) {
		final String val = attrs.get(name);
		if (val == null) {
			LOG.severe("Null for " + name);
		}
		return val;
	}

	private Set<Long> f_unreferencedObjects;

	protected void newObject(long id) {
		f_unreferencedObjects.add(id);
	}

	protected void useObject(long id) {
		f_unreferencedObjects.remove(id);
	}

	private Set<Long> f_unreferencedFields;

	protected void newField(long id) {
		f_unreferencedFields.add(id);
	}

	protected void useField(long id) {
		f_unreferencedFields.remove(id);
	}

	public void setup(Connection c, Timestamp start, long startNS,
			ScanRawFilePreScan scanResults, Set<Long> unreferencedObjects,
			Set<Long> unreferencedFields) throws SQLException {
		f_unreferencedObjects = unreferencedObjects;
		f_unreferencedFields = unreferencedFields;
	}

	public void flush(final int runId, final long endTime) throws SQLException {
		// Nothing to do
	}

	public void printStats() {
		// Nothing to do
	}
}
