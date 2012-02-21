package com.surelogic._flashlight.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public enum OutputType {

    FL(false, false, ".fl"), FL_GZ(false, true, ".fl.gz"), FLB(true, false,
            ".flb"), FLB_GZ(true, true, ".flb.gz"), FLH(false, false, ".flh");

    private final boolean binary, compressed;
    private final String suffix;

    private OutputType(final boolean bin, final boolean gz, final String sf) {
        binary = bin;
        compressed = gz;
        suffix = sf;
    }

    public boolean isBinary() {
        return binary;
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

    public static OutputType get(final String useBinary, final boolean compress) {
        boolean binary = "true".equals(useBinary);
        for (OutputType val : values()) {
            if (val.isBinary() == binary && val.isCompressed() == compress) {
                return val;
            }
        }
        return FL_GZ;
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
            stream = new GZIPInputStream(stream, 32 * 1024);
        } else {
            stream = new BufferedInputStream(stream, 32 * 1024);
        }
        if (type.isBinary()) {
            stream = new ObjectInputStream(stream);
        }
        return stream;
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
            stream = new GZIPOutputStream(stream);
        } else {
            stream = new BufferedOutputStream(stream);
        }
        if (type.isBinary()) {
            stream = new ObjectOutputStream(stream);
        }
        return stream;
    }
}
