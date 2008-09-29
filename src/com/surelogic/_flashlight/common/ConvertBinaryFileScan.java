package com.surelogic._flashlight.common;

import java.io.*;
import java.util.zip.GZIPOutputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.CharBuffer;
import com.surelogic.common.xml.Entities;

public class ConvertBinaryFileScan extends DefaultHandler {
	final PrintWriter f_out;
	final CharBuffer f_buf = new CharBuffer();
	
	public ConvertBinaryFileScan(File convertedFile) throws IOException {
		if (convertedFile.exists()) {
			throw new IOException("Already exists: "+convertedFile);
		}
		FileOutputStream fos = new FileOutputStream(convertedFile);
		GZIPOutputStream gos = new GZIPOutputStream(fos);
		OutputStreamWriter fw = new OutputStreamWriter(gos);		
		f_out = new PrintWriter(fw);		
	}

	public void startElement(String uri, String localName,
			                 String qName, Attributes attributes) throws SAXException
	{
		f_buf.clear();
		f_buf.append('<').append(qName);
		final int numAttrs = attributes.getLength();
		for(int i=0; i<numAttrs; i++) {
			String name  = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			f_buf.append(' ').append(name).append("='");
			Entities.addEscaped(value, f_buf).append('\'');
		}
		f_buf.append("/>\n");
		f_buf.write(f_out);
	}
	
	public void close() {
		f_out.close();
	}
}
