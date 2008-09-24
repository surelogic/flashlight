package com.surelogic._flashlight.common;

import java.io.*;
import java.util.HashMap;

import org.xml.sax.Attributes;

public class BinaryAttributes extends HashMap<IAttributeType,Object> implements Attributes {
	private static final long serialVersionUID = -236988557562438004L;

	public int getIndex(String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getIndex(String uri, String localName) {
		throw new UnsupportedOperationException();
	}

	public int getLength() {
		// TODO Auto-generated method stub
		return 0; // USED
	}

	public String getLocalName(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getQName(int index) {
		// TODO Auto-generated method stub
		return null; // USED
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
		// TODO Auto-generated method stub
		return null;
	}

	public String getValue(int index) {
		// TODO Auto-generated method stub
		return null; // USED
	}

	public String getValue(String name) {
		// TODO Auto-generated method stub
		return null; 
	}

	public String getValue(String uri, String localName) {
		throw new UnsupportedOperationException();
	}

	public void readAttributes(ObjectInputStream in, EventType event) throws IOException {
		event.read(in, this);
	}

}
