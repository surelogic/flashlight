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
import java.util.EnumSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public enum OutputType {

  FL(false, ".fl"), FL_GZ(true, ".fl.gz"), FLH(false, ".flh"), COMPLETE(false, ".complete");

  /**
   * A set that indicates raw data files.
   */
  public static final EnumSet<OutputType> RAW_DATA = EnumSet.of(FL, FL_GZ);

  private final boolean f_compressed;
  private final String f_suffix;

  private OutputType(final boolean gz, final String sf) {
    f_compressed = gz;
    f_suffix = sf;
  }

  public boolean isCompressed() {
    return f_compressed;
  }

  /* @NonNull */
  public String getSuffix() {
    return f_suffix;
  }

  /**
   * Returns an output type in {@link #values()} that exactly matches the passed
   * string, or the passed default.
   * 
   * @param value
   *          a string.
   * @param defaultValue
   *          a default output type.
   * @return an output type in {@link #values()} that exactly matches the passed
   *         string, or the passed default.
   */
  public static OutputType valueOf(final String value, /* @NonNull */final OutputType defaultValue) {
    if (value != null) {
      for (OutputType val : values()) {
        if (val.toString().equals(value)) {
          return val;
        }
      }
    }
    return defaultValue;
  }

  /**
   * Gets the output type for a raw file based upon if it should be compressed
   * or not.
   * 
   * @param compress
   *          {@code true} if compression should be used, {@code false}
   *          otherwise.
   * @return {@link #FL_GZ} if compression is requested, {@link #FL} if not.
   */
  /* @NonNull */
  public static OutputType get(final boolean compress) {
    if (compress) {
      return FL_GZ;
    } else {
      return FL;
    }
  }

  /**
   * Gets an output type for the passed file, or {@code null} if none can be
   * determined.
   * 
   * @param file
   *          any file.
   * @return an output type for the passed file, or {@code null} if none can be
   *         determined.
   */
  /* @Nullable */
  public static OutputType detectFileType(final File file) {
    if (file == null)
      return null;
    final String name = file.getName();
    for (OutputType t : values()) {
      if (name.endsWith(t.getSuffix())) {
        return t;
      }
    }
    return null;
  }

  /**
   * Checks if the passed file may be a raw data file based upon its suffix.
   * 
   * @param file
   *          any file
   * @return {@code true} if the passed file may be a raw data file based upon
   *         its suffix, {@code false} otherwise.
   */
  public static boolean mayBeRawDataFile(final File file) {
    if (file == null)
      return false;
    final OutputType type = detectFileType(file);
    return RAW_DATA.contains(type);
  }

  /**
   * Gets an input stream to read the passed raw file. This method opens the
   * right kind of stream based upon if the raw file is compressed or not.
   * 
   * @param dataFile
   *          a raw data file.
   * @return an input stream to read the passed raw file.
   * @throws IOException
   *           if the file doesn't exist or some other IO problem occurs.
   */
  public static InputStream getInputStreamFor(final File dataFile) throws IOException {
    return getInputStreamFor(new FileInputStream(dataFile), OutputType.detectFileType(dataFile));
  }

  /**
   * Gets an input stream to read the passed raw file. This method opens the
   * right kind of stream based upon if the raw file is compressed or not.
   * 
   * @param dataFile
   *          a raw data file.
   * @return an input stream to read the passed raw file.
   * @throws IOException
   *           if the file doesn't exist or some other IO problem occurs.
   */
  public static InputStream getInputStreamFor(InputStream stream, final OutputType type) throws IOException {
    if (type.isCompressed()) {
      return new GZIPInputStream(stream, 32 * 1024);
    } else {
      return new BufferedInputStream(stream, 32 * 1024);
    }
  }

  /**
   * Gets an output stream to write to based on the name of the given data file.
   * This method opens the right kind of stream based on whether the file suffix
   * indicates binary and/or compressed output.
   * 
   * @param dataFile
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static OutputStream getOutputStreamFor(final File dataFile) throws IOException {
    return getOutputStreamFor(new FileOutputStream(dataFile), detectFileType(dataFile));
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
  public static OutputStream getOutputStreamFor(OutputStream stream, final OutputType type) throws IOException {
    if (type.isCompressed()) {
      return new GZIPOutputStream(stream);
    } else {
      return new BufferedOutputStream(stream);
    }
  }

  public static SAXParser getParser(final OutputType type) throws ParserConfigurationException, SAXException {
    return SAXParserFactory.newInstance().newSAXParser();
  }

  public static SAXParser getParser(final File dataFile) throws ParserConfigurationException, SAXException {
    return getParser(detectFileType(dataFile));
  }
}
