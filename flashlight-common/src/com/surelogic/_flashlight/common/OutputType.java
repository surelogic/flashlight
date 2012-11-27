package com.surelogic._flashlight.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public enum OutputType {

    FL(false, ".fl"), FL_GZ(true, ".fl.gz"), FLH(false, ".flh");

    private final boolean compressed;
    private final String suffix;

    private OutputType(final boolean gz, final String sf) {
        compressed = gz;
        suffix = sf;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public String getSuffix() {
        return suffix;
    }

    public static OutputType valueOf(final String name,
            final OutputType defValue) {
        if (name != null) {
            for (OutputType val : values()) {
                if (val.toString().equals(name)) {
                    return val;
                }
            }
        }
        return defValue;
    }

    public static OutputType get(final boolean compress) {
        if (compress) {
            return FL_GZ;
        } else {
            return FL;
        }
    }

    public static OutputType detectFileType(final File dataFile) {
        final String name = dataFile.getName();
        for (OutputType t : values()) {
            if (name.endsWith(t.getSuffix())) {
                return t;
            }
        }
        return null;
    }

    /**
     * Gets an input stream to read the passed raw file. This method opens the
     * right kind of stream based upon if the raw file is compressed or not.
     * 
     * @param dataFile
     *            a raw data file.
     * @return an input stream to read the passed raw file.
     * @throws IOException
     *             if the file doesn't exist or some other IO problem occurs.
     */
    public static InputStream getInputStreamFor(final File dataFile)
            throws IOException {
        return getInputStreamFor(new FileInputStream(dataFile),
                OutputType.detectFileType(dataFile));
    }

    /**
     * Gets an input stream to read the passed raw file. This method opens the
     * right kind of stream based upon if the raw file is compressed or not.
     * 
     * @param dataFile
     *            a raw data file.
     * @return an input stream to read the passed raw file.
     * @throws IOException
     *             if the file doesn't exist or some other IO problem occurs.
     */
    public static InputStream getInputStreamFor(InputStream stream,
            final OutputType type) throws IOException {
        if (type.isCompressed()) {
            return new GZIPInputStream(stream, 32 * 1024);
        } else {
            return new BufferedInputStream(stream, 32 * 1024);
        }
    }

    /**
     * Gets an output stream to write to based on the name of the given data
     * file. This method opens the right kind of stream based on whether the
     * file suffix indicates binary and/or compressed output.
     * 
     * @param dataFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static OutputStream getOutputStreamFor(final File dataFile)
            throws IOException {
        return getOutputStreamFor(new FileOutputStream(dataFile),
                detectFileType(dataFile));
    }

    /**
     * Wraps an output stream based on whether the file suffix indicates binary
     * and/or compressed output.
     * 
     * @param stream
     * @param type
     * @return
     * @throws IOException
     */
    public static OutputStream getOutputStreamFor(OutputStream stream,
            final OutputType type) throws IOException {
        if (type.isCompressed()) {
            return new GZIPOutputStream(stream);
        } else {
            return new BufferedOutputStream(stream);
        }
    }

    public static SAXParser getParser(final OutputType type)
            throws ParserConfigurationException, SAXException {
        return SAXParserFactory.newInstance().newSAXParser();
    }

    public static SAXParser getParser(final File dataFile)
            throws ParserConfigurationException, SAXException {
        return getParser(detectFileType(dataFile));
    }
}
