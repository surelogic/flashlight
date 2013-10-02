package com.surelogic.flashlight.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GenerateHtmlDocs extends DefaultHandler {

    public static void main(String[] args) throws ParserConfigurationException,
            SAXException, IOException {
        SAXParserFactory fact = SAXParserFactory.newInstance();
        SAXParser parser = fact.newSAXParser();
        InputStream stream = new FileInputStream(
                "lib/adhoc/default-flashlight-queries.xml");
        try {
            parser.parse(stream, new GenerateHtmlDocs());
        } finally {
            stream.close();
        }
    }

    private final PrintWriter writer;
    private String name;
    private final StringBuilder chars = new StringBuilder();

    GenerateHtmlDocs() throws FileNotFoundException, IOException {
        File temp = File.createTempFile("queries", ".html");
        System.out.printf("Writing to %s.\n", temp);
        writer = new PrintWriter(temp);
        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"> "
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> "
                + "\n<head><title></title></head><body>");
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (name != null) {
            chars.append(ch, start, length);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if ("query".equals(qName)) {
            name = attributes.getValue("description");
            if (name == null) {
                throw new IllegalStateException("description must not be null.");
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (name != null) {
            int start = chars.indexOf("META-BEGIN(doc)");
            int end = chars.indexOf("META-END");
            if (start >= 0 && end >= 0) {
                writer.println(chars.substring(
                        start + "META-BEGIN(doc)".length(), end).replaceAll(
                        "--( )?", ""));
            } else {
                writer.printf("<p><strong>\n\t%s\n</strong></p>\n", name);
            }
        }
        chars.setLength(0);
        name = null;
    }

    @Override
    public void endDocument() throws SAXException {
        writer.println("</body></html>");
        writer.close();
    }

}
