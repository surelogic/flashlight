package com.surelogic._flashlight.common;

import java.io.*;

import org.xml.sax.*;

public class BinaryEventReader {	
	public void parse(ObjectInputStream in, ContentHandler handler) throws SAXException, IOException {
		final BinaryAttributes attrs = new BinaryAttributes();
		handler.startDocument();
		
		int type;
		while ((type = in.read()) >= 0) {
			final EventType event = EventType.getEvent(type);
			final String name     = event.getLabel();
			attrs.readAttributes(in, event); 
			handler.startElement(null, name, name, attrs);
			handler.endElement(null, name, name);		
		}
		handler.endDocument();
	}
}
