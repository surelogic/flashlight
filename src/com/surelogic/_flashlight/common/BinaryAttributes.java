package com.surelogic._flashlight.common;

import java.io.*;
import java.util.*;

import org.xml.sax.Attributes;

public class BinaryAttributes extends HashMap<IAttributeType,Object> implements Attributes {
	private static final long serialVersionUID = -236988557562438004L;	
	static final boolean debug = BinaryEventReader.debug;
	
	/*
	BinaryAttributes() {
		super(IAttributeType.comparator);
	}
	*/
	private Map.Entry<IAttributeType,Object>[] entries;
	
	@SuppressWarnings("unchecked")
	private void initEntries() {
		if (entries != null) {
			return;
		}
		entries = new Map.Entry[this.size()];
		int i=0;
		for(Map.Entry<IAttributeType,Object> e : this.entrySet()) {
			entries[i] = e;
			i++;
		}
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
		return getValue(getIndex(name)); 
	}

	public String getValue(String uri, String localName) {
		throw new UnsupportedOperationException();
	}

	public void readAttributes(ObjectInputStream in, EventType event) throws IOException {
		event.read(in, this);
		preprocess(event);
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
}
