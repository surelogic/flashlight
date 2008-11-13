package com.surelogic._flashlight;

import com.surelogic._flashlight.common.IdConstants;

/**
 * This class is giant hack, but I don't know of a better way to do things.  
 * This class cannot be instantiated, but contains various configuration
 * attributes for the {@link Store} class.  These attributes must be set 
 * <em>before</em> the {@code Store} class is initialized by the virtual
 * machine because the attributes are accessed by the <code>static</code>
 * initializer of the class.  
 * 
 * <p>This class initializes the attributes from Java System properties, but
 * these values can be overridden using the various setter methods.
 */
public class StoreConfiguration {
  private static final String FL_OFF = "FL_OFF";

  private static final String FL_DIR = "FL_DIR";
  private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

  private static final String FL_RUN = "FL_RUN";
  private static final String FL_RUN_DEFAULT = "flashlight";

  private static final String FL_RAWQ_SIZE = "FL_RAWQ_SIZE";
  private static final int FL_RAWQ_SIZE_DEFAULT = IdConstants.useRefinery ? 16 : 200;

  private static final String FL_OUTQ_SIZE = "FL_OUTQ_SIZE";
  private static final int FL_OUTQ_SIZE_DEFAULT = 16;

  private static final String FL_REFINERY_SIZE = "FL_REFINERY_SIZE";
  private static final int FL_REFINERY_SIZE_DEFAULT = 250;

  private static final String FL_NO_SPY = "FL_NO_SPY";
  
  private static final String FL_CONSOLE_PORT = "FL_CONSOLE_PORT";
  private static final int FL_CONSOLE_PORT_DEFAULT = 43524;

  private static final String FL_FIELDS_FILE = "FL_FIELDS_FILE";

  private static final String FL_SITES_FILE = "FL_SITES_FILE";
  
  private static final String FL_FILTERS_FILE = "FL_FILTERS_FILE";
  
  private static final String FL_DATE_OVERRIDE = "FL_DATE_OVERRIDE";
  
  private static volatile boolean isOff;
  private static volatile String directory;
  private static volatile String runName;
  private static volatile int rawQueueSize;
  private static volatile int outQueueSize;
  private static volatile int refinerySize;
  private static volatile boolean noSpy;
  private static volatile int consolePort;
  private static volatile String fieldsFile;
  private static volatile String sitesFile;
  private static volatile String filtersFile;
  private static volatile String dateOverride;
  
  static {
    // Initialize the settings base on Java System properties
    setOff(System.getProperty(FL_OFF, null) != null);
    setDirectory(System.getProperty(FL_DIR, System.getProperty(JAVA_IO_TMPDIR)));
    setRun(System.getProperty(FL_RUN, FL_RUN_DEFAULT));
    setRawQueueSize(getIntProperty(FL_RAWQ_SIZE, FL_RAWQ_SIZE_DEFAULT));
    setOutQueueSize(getIntProperty(FL_OUTQ_SIZE, FL_OUTQ_SIZE_DEFAULT));
    setRefinerySize(getIntProperty(FL_REFINERY_SIZE, FL_REFINERY_SIZE_DEFAULT));
    setNoSpy(System.getProperty(FL_NO_SPY) != null);
    setConsolePort(getIntProperty(FL_CONSOLE_PORT, FL_CONSOLE_PORT_DEFAULT));
    setFieldsFile(System.getProperty(FL_FIELDS_FILE));
    setSitesFile(System.getProperty(FL_SITES_FILE));
    setFiltersFile(System.getProperty(FL_FILTERS_FILE));
    setDateOverride(System.getProperty(FL_DATE_OVERRIDE));
  }
  
  private static int getIntProperty(final String key, int def) {
    try {
      String intString = System.getProperty(key);
      if (intString != null) def = Integer.parseInt(intString);
    } catch (NumberFormatException e) {
      // ignore, go with the default
    }
    return def;
  }

  // Private constructor to prevent instantiation
  private StoreConfiguration() {
    // do nothing
  }
  
 
  
  /**
   * Is the instrumentation disabled?
   * 
   * <P>
   * This value is initialized from the Java system property <code>FL_OFF</code>:
   * If the property has a value, then the instrumentation is turned off.
   */
  public static boolean isOff() {
    return isOff;
  }

  /**
   * Set whether the instrumentation is disabled.
   * @param flag {@code true} if the instrumentation should be disabled.
   */
  public static void setOff(final boolean flag) {
    isOff = flag;
  }
  
  /**
   * Get the directory to write the data into.
   * 
   * <p>
   * Thie value is initialized from the Java system property <code>FL_DIR</code>
   * with the default value coming from the <code>java.io.tmpdir</code>
   * property.
   */
  public static String getDirectory() {
    return directory;
  }

  /**
   * Set the data directory.
   */
  public static void setDirectory(final String dir) {
    directory = dir;
  }
  
  /**
   * Get the file prefix&mdash;run id&mdash;for the flashlight data file.
   * 
   * <p>
   * This value is initialized from the Java system property <code>FL_RUN</code>
   * with the default value of <code>"flashlight"</code>.
   */
  public static String getRun() {
    return runName;
  }
  
  /**
   * Set prefix for the data file.
   */
  public static void setRun(final String name) {
    runName = name;
  }
  
  /**
   * Get the size of the BlockingQueue between the instrumentation and the
   * refinery (which deals with garbage collection). Many threads input and one
   * thread drains this queue.
   * 
   * <p>
   * This value is initialized from the Java system property
   * <code>FL_RAWQ_SIZE</code> with the default value of 500.
   */
  public static int getRawQueueSize() {
    return rawQueueSize;
  }
  
  /**
   * Set the size of the BlockingQueue between the instrumentation and the
   * refinery (which deals with garbage collection). Many threads input and one
   * thread drains this queue.  The size is always at least 10.
   */
  public static void setRawQueueSize(final int size) {
    rawQueueSize = Math.max(size, 10);
  }
  
  /**
   * Get the size of the BlockingQueue between the refinery and the depository
   * (which deals with output). One thread inputs and another thread drains this
   * queue.
   * 
   * <p>
   * This value is initialized from the Java system property
   * <code>FL_OUTQ_SIZE</code> with the default value of 500.
   */
  public static int getOutQueueSize() {
    return outQueueSize;
  }
  
  /**
   * Set the size of the BlockingQueue between the refinery and the depository
   * (which deals with output). One thread inputs and another thread drains this
   * queue.  The size is always at least 10.
   */
  public static void setOutQueueSize(final int size) {
    outQueueSize = Math.max(size, 10);
  }
  
  /**
   * Get the size of the List used to cache events in the refinery. This is the
   * number of events the refinery holds on to before it puts events in to the
   * output queue. If the garbage collector notifies the store that an object is
   * garbage collected, and that object is thread local, then all the events
   * about that object that are held in the refinery can be discarded (and
   * therefore not output).
   * 
   * <p>
   * This value is initialized from the Java system property
   * <code>FL_REFINERY_SIZE</code> with the default value of 5000.
   */
  public static int getRefinerySize() {
    return refinerySize;
  }

  /**
   * Set the size of the List used to cache events in the refinery. This is the
   * number of events the refinery holds on to before it puts events in to the
   * output queue. If the garbage collector notifies the store that an object is
   * garbage collected, and that object is thread local, then all the events
   * about that object that are held in the refinery can be discarded (and
   * therefore not output).  The size is always at least 100.
   */
  public static void setRefinerySize(final int size) {
    refinerySize = Math.max(size, 100);
  }
  
  /**
   * Is the "spy thread" disabled? This thread checks to see if Flashlight
   * threads are all that is running. This is useful if a program just
   * terminates without a call to System.exit()
   * 
   * <P>
   * This value is initialized from the Java system property <code>FL_NO_SPY</code>:
   * If the property has a value, then the instrumentation is turned off.
   */
  public static boolean getNoSpy() {
    return noSpy;
  }
  
  /**
   * Set whether the spy thread is disabled.
   */
  public static void setNoSpy(final boolean flag) {
    noSpy = flag;
  }
  
  /**
   * The port that a console can connect to to shutdown the instrumentation. If
   * the port specified is used the program counts up...it tries the next 100
   * and then gives up.
   * 
   * <P>
   * This value is initialized from the Java system property
   * <code>FL_CONSOLE_PORT</code> with the default value of 43524.
   */
  public static int getConsolePort() {
    return consolePort;
  }

  /**
   * Set the initial value for the console port. If
   * the port specified is used the program counts up...it tries the next 100
   * and then gives up.  This value must be at least 1024.
   */
  public static void setConsolePort(final int port) {
    consolePort = Math.max(port, 1024);
  }
  
  public static String getFieldsFile() {
	  return fieldsFile;
  }
  
  public static void setFieldsFile(final String file) {
    fieldsFile = file;		
  }
  
  public static String getSitesFile() {
    return sitesFile;
  }
  
  public static void setSitesFile(final String file) {
    sitesFile = file;   
  }
  
  public static String getFiltersFile() {
	return filtersFile;
  }

  public static void setFiltersFile(final String file) {
	filtersFile = file;   
  }
  
  public static String getDateOverride() {
    return dateOverride;
  }
  
  public static void setDateOverride(final String date) {
    dateOverride = date;
  }
}
