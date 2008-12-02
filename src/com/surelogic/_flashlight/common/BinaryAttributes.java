package com.surelogic._flashlight.common;

import java.io.*;
import java.util.*;

import org.xml.sax.Attributes;

public class BinaryAttributes extends PreppedAttributes implements Attributes {
	private static final long serialVersionUID = -236988557562438004L;	
	static final boolean debug = BinaryEventReader.debug;
	static final boolean mergePersistentValues = false;
	
	private final Map<IAttributeType,Object> persistent = new HashMap<IAttributeType,Object>(4);
	/*
	BinaryAttributes() {
		super(IAttributeType.comparator);
	}
	*/	
	private Map.Entry<IAttributeType,Object>[] entries;
	private final boolean showRawData;
	/**
	 * Special-case fields for very common attributes 
	 */
	private long startTime;
	private long eventTime;
	private long traceId;
	private long lockId;
	private long threadId;
	
	BinaryAttributes(boolean raw) {
		showRawData = raw;
	}
	
	@SuppressWarnings("unchecked")
	private void initEntries() {
		if (entries != null) {
			return;
		}
		final int size;
		if (mergePersistentValues) {
			size = this.size();
		} else {
			size = this.size() + persistent.size();
		}
		entries = new Map.Entry[size];
		final int i = appendEntries(0, this);
		if (!mergePersistentValues) {
			appendEntries(i, persistent);
		}
	}

	private int appendEntries(int i, Map<IAttributeType,Object> map) {
		for(Map.Entry<IAttributeType,Object> e : map.entrySet()) {
			entries[i] = e;
			i++;
		}
		return i;
	}
	
	public int getIndex(String name) {
		initEntries();
		int i=0;
		for(Map.Entry<IAttributeType,Object> e : entries) {
			if (e.getKey().label().equals(name)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public int getIndex(String uri, String localName) {
		throw new UnsupportedOperationException();
	}

	public int getLength() {
		return this.size();
	}

	public String getLocalName(int index) {
		return getQName(index);
	}

	public String getQName(int i) {
		initEntries();
		if (i >= entries.length) {
			System.out.println(this);
		}
		return entries[i].getKey().label();
	}

	public String getType(int index) {
		throw new UnsupportedOperationException();
	}

	public String getType(String name) {
		throw new UnsupportedOperationException();
	}

	public String getType(String uri, String localName) {
		throw new UnsupportedOperationException();
	}

	public String getURI(int index) {
		throw new UnsupportedOperationException();
	}

	public String getValue(int i) {
		initEntries();
		return entries[i].getValue().toString();
	}

	public String getValue(String name) {
		Object o = get(PreppedAttributes.mapAttr(name)); 
		return o == null ? null : o.toString(); 
	}

	public String getValue(String uri, String localName) {
		throw new UnsupportedOperationException();
	}

	public void readAttributes(ObjectInputStream in, EventType event) throws IOException {
		event.read(in, this);
		if (!showRawData) {
			preprocess(event);
		}
	}

	@Override
	public Object get(Object key) {
		if (mergePersistentValues || showRawData) {
			return super.get(key);
		}
		Object val = super.get(key);
		if (val == null) {
			val = persistent.get(key);
		}
		return val;		
	}
	
	@Override
	public Object put(IAttributeType key, Object value) {
		if (debug) System.out.println("Got attr: "+key.label()+" -> "+value);
		return super.put(key, value);
	}
	
	@Override
	public void clear() {
		super.clear();
		entries = null;
		eventTime = -1;
		traceId = IdConstants.ILLEGAL_ID;
	}

	/**
	 * Synthesize any attributes if necessary
	 */ 
	private void preprocess(EventType event) {
		switch (event) {
		case Observed_CallLocation:
			// Cache location based on IN_CLASS and LINE
			CallLocation newLoc = new CallLocation(this);
			locCache.put(newLoc, newLoc);
			break;
		case Before_Trace:
			CallLocation loc = locCache.get(new CallLocation(this));
			this.put(AttributeType.FILE, loc.getFile());
			this.put(AttributeType.LOCATION, loc.getLocation());
			break;
		default:
		}
	}

	private final Map<CallLocation,CallLocation> locCache = 
		new HashMap<CallLocation, CallLocation>();
	
	static class CallLocation {
		final long classId;
		final int line;
		final String file;
		final String location;
		
		public CallLocation(BinaryAttributes attrs) {
			classId  = ((Long) attrs.get(AttributeType.IN_CLASS)).longValue();
			line     = ((Integer) attrs.get(AttributeType.LINE)).intValue();
			location = (String) attrs.get(AttributeType.LOCATION);
			file     = (String) attrs.get(AttributeType.FILE);
		}
		
		public String getLocation() {
			// TODO Auto-generated method stub
			return location;
		}

		public String getFile() {
			return file;
		}
		
		@Override
		public final int hashCode() {
			return (int) classId + line;
		}
		
		@Override
		public final boolean equals(Object o) {
			if (o instanceof CallLocation) {
				CallLocation loc = (CallLocation) o;
				return classId == loc.classId && line == loc.line;
			}
			return false;
		}
	}

	/**
	 * Like clear(), but also re-initializes certain attributes
	 * that are marked as 'persistent'
	 */
	public void reset(EventType event) {
		if (showRawData) {
			clear();
			return;
		}
		// Save attributes that the event marks as persistent
		final IAttributeType attr = event.getPersistentAttribute();
		if (attr != null) {
			persistent.put(attr, this.get(attr));
		}		
		// Clear the rest
		clear();

		if (mergePersistentValues) {
			// Reinit persistent attributes
			putAll(persistent);
		}
	}
	
	@Override
	public long getLong(IAttributeType key) {
		Object o = this.get(key);
		if (o == null) {
			return Long.MIN_VALUE;
		}
		return (Long) o; 
	}
	
	public void setStartTime(long time) {
		startTime = time;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public void setEventTime(long time) {
		eventTime = time;
	}
	@Override
	public long getEventTime() {
		return eventTime;
	}
	
	public void setTraceId(long id) {
		traceId = id;
	}	
	@Override
	public long getTraceId() {
		return traceId;
	}
	
	public void setLockId(long l) {
		lockId = l;
	}
	@Override
	public long getLockId() {
		return lockId;
	}
	
	public void setThreadId(long t) {
		threadId = t;
	}
	@Override
	public long getThreadId() {	
		return threadId;
	}
}
