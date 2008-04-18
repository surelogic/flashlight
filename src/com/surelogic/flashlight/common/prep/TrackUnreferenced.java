package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import org.xml.sax.Attributes;

public abstract class TrackUnreferenced implements IPrep {
	protected static final String LINE = "line";
	protected static final String FILE = "file";
	protected static final String THREAD = "thread";
	protected static final String NANO_TIME = "nano-time";
	
    private final Map<String,String> attrs = new HashMap<String,String>();
	
    protected void parseAttrs(Attributes attributes) {
    	attrs.clear();
    	if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				final String aName = attributes.getQName(i);
				final String aValue = attributes.getValue(i);
				attrs.put(aName, aValue);
			}
    	}
    }
    
    protected String getAttr(String name) {
    	return attrs.get(name);
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
			DataPreScan scanResults, Set<Long> unreferencedObjects,
			Set<Long> unreferencedFields) throws SQLException {
		f_unreferencedObjects = unreferencedObjects;
		f_unreferencedFields = unreferencedFields;
	}
	
	public void flush(final int runId) throws SQLException {
		// Nothing to do
	}
	
	public void printStats() {
		// Nothing to do
	}
}
