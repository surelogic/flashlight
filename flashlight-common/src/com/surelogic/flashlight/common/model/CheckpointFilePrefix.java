package com.surelogic.flashlight.common.model;

import java.io.File;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * The prefix information of a raw data file. The {@link #read(File)} method
 * loads in the information from a specified raw data file.
 */
public final class CheckpointFilePrefix {

  private File f_dataFile;

  public File getFile() {
    return f_dataFile;
  }

  private String f_name;

  public String getName() {
    return f_name;
  }

  private String f_rawDataVersion;

  public String getRawDataVersion() {
    return f_rawDataVersion;
  }

  private String f_hostname;

  public String getHostname() {
    return f_hostname;
  }

  private String f_userName;

  public String getUserName() {
    return f_userName;
  }

  private String f_javaVersion;

  public String getJavaVersion() {
    return f_javaVersion;
  }

  private String f_javaVendor;

  public String getJavaVendor() {
    return f_javaVendor;
  }

  private String f_osName;

  public String getOSName() {
    return f_osName;
  }

  private String f_osArch;

  public String getOSArch() {
    return f_osArch;
  }

  private String f_osVersion;

  public String getOSVersion() {
    return f_osVersion;
  }

  private int f_maxMemoryMb;

  public int getMaxMemoryMb() {
    return f_maxMemoryMb;
  }

  private int f_processors;

  public int getProcessors() {
    return f_processors;
  }

  private Timestamp f_started;

  public Timestamp getStartTimeOfRun() {
    return f_started;
  }

  private long f_nanoTime;

  public long getNanoTime() {
    return f_nanoTime;
  }

  private Date f_wallClockTime;

  public Date getWallClockTime() {
    return f_wallClockTime;
  }

  private boolean f_android = false;

  public boolean isAndroid() {
    return f_android;
  }

  /**
   * Checks whether or not this object is well-formed. All attributes are
   * considered except for duration, which was a later addition to this object.
   */
  boolean isWellFormed() {
    if (f_name == null) {
      return false;
    }
    if (f_rawDataVersion == null) {
      return false;
    }
    if (f_hostname == null) {
      return false;
    }
    if (f_userName == null) {
      return false;
    }
    if (f_javaVersion == null) {
      return false;
    }
    if (f_javaVendor == null) {
      return false;
    }
    if (f_osName == null) {
      return false;
    }
    if (f_osArch == null) {
      return false;
    }
    if (f_osVersion == null) {
      return false;
    }
    if (f_maxMemoryMb == 0) {
      return false;
    }
    if (f_processors == 0) {
      return false;
    }
    if (f_nanoTime == 0) {
      return false;
    }
    if (f_wallClockTime == null) {
      return false;
    }
    return true;
  }

  private class PrefixHandler extends DefaultHandler {
    @Override
    public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
        throws SAXException {
      boolean isPrefixElement = name.equals("flashlight") || name.equals("environment") || name.equals("time");
      if (!isPrefixElement) {
        /*
         * Stop reading the file, we are past the information we want to read.
         * This is a bit of a hack, but it is the only way to tell the parser to
         * stop.
         */
        throw new SAXException("done");
      }
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); i++) {
          final String aName = attributes.getQName(i);
          final String aValue = attributes.getValue(i);
          if (AttributeType.RUN.label().equals(aName)) {
            f_name = aValue;
          } else if (AttributeType.VERSION.label().equals(aName)) {
            f_rawDataVersion = aValue;
          } else if (AttributeType.HOSTNAME.label().equals(aName)) {
            f_hostname = aValue;
          } else if (AttributeType.USER_NAME.label().equals(aName)) {
            f_userName = aValue;
          } else if (AttributeType.JAVA_VERSION.label().equals(aName)) {
            f_javaVersion = aValue;
          } else if (AttributeType.JAVA_VENDOR.label().equals(aName)) {
            f_javaVendor = aValue;
          } else if (AttributeType.OS_NAME.label().equals(aName)) {
            f_osName = aValue;
          } else if (AttributeType.OS_ARCH.label().equals(aName)) {
            f_osArch = aValue;
          } else if (AttributeType.OS_VERSION.label().equals(aName)) {
            f_osVersion = aValue;
          } else if (AttributeType.MEMORY_MB.label().equals(aName)) {
            f_maxMemoryMb = Integer.parseInt(aValue);
          } else if (AttributeType.CPUS.label().equals(aName)) {
            f_processors = Integer.parseInt(aValue);
          } else if (AttributeType.TIME.label().equals(aName)) {
            f_nanoTime = Long.parseLong(aValue);
          } else if (AttributeType.WALL_CLOCK.label().equals(aName)) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {
              f_wallClockTime = dateFormat.parse(aValue);
            } catch (final ParseException e) {
              throw new SAXException(e);
            }
          } else if (AttributeType.ANDROID.label().equals(aName)) {
            f_android = true;
          }
        }
      }
    }
  }

  /**
   * Reads the prefix information about the passed raw data file into this.
   * 
   * @param dataFile
   *          the file to read.
   * @throws Exception
   *           if something goes wrong trying to read the file.
   */
  public void read(final File dataFile) {
    if (dataFile == null) {
      throw new IllegalArgumentException(I18N.err(44, "dataFile"));
    }
    try {

      if (!dataFile.exists()) {
        SLLogger.getLogger().log(Level.SEVERE, I18N.err(106, dataFile.getName()));
        return;
      }
      f_dataFile = dataFile;

      final InputStream stream = OutputType.getInputStreamFor(dataFile);
      try {

        /*
         * Read the flashlight, environment, and time elements from the data
         * file.
         */
        final PrefixHandler handler = new PrefixHandler();
        try {
          // Parse the input
          final SAXParser saxParser = OutputType.getParser(f_dataFile);
          saxParser.parse(stream, handler);
        } catch (final SAXException e) {
          /*
           * Ignore, this is expected because we don't want to parse the entire
           * really-really huge file. However, make sure we got what we wanted.
           */
          if (!e.getMessage().equals("done")) {
            SLLogger.getLogger().log(Level.FINE, I18N.err(109, dataFile.getAbsolutePath()));
            throw new SAXException(e);
          }
        }
      } finally {
        stream.close();
      }
    } catch (Exception e) {
      SLLogger.getLogger().log(Level.FINE, I18N.err(105, dataFile.getAbsolutePath()), e);
    }
  }
}
