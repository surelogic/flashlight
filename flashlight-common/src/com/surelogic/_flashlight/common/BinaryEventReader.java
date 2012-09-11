package com.surelogic._flashlight.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class BinaryEventReader extends SAXParser {
    static final boolean debug = false;
    boolean showRawData = false;

    public void showRawData(boolean b) {
        showRawData = b;
    }

    @Override
    public void parse(InputStream in, DefaultHandler handler)
            throws SAXException, IOException {
        final ObjectInputStream oin = (ObjectInputStream) in;
        final BinaryAttributes attrs = new BinaryAttributes(showRawData);
        handler.startDocument();
        int type;
        while ((type = in.read()) >= 0) {
            final EventType event = EventType.getEvent(type);
            final String name = event.getLabel();
            if (debug) {
                System.out.println("Got event: " + name + " (" + type + ")");
            }
            attrs.readAttributes(oin, event);
            if (event.processEvent()) {
                handler.startElement(null, name, name, attrs);
            }
            attrs.reset(event);
            if (event.processEvent()) {
                handler.endElement(null, name, name);
            }
        }
        handler.endDocument();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Parser getParser() throws SAXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMLReader getXMLReader() throws SAXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNamespaceAware() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValidating() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new UnsupportedOperationException();
    }
}
