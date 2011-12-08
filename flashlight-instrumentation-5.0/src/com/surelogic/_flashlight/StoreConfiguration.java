package com.surelogic._flashlight;

import static com.surelogic._flashlight.common.InstrumentationConstants.DATE_FORMAT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_COLLECTION_TYPE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_COLLECTION_TYPE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_CONSOLE_PORT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_CONSOLE_PORT_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DATE_OVERRIDE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DEBUG;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_DIR;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_FIELDS_FILE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_FIELDS_FILE_NAME;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_FIELDS_RESOURCE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_LOG_FILE_NAME;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_LOG_RESOURCE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_NO_SPY;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OFF;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTPUT_TYPE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTPUT_TYPE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTQ_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_OUTQ_SIZE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_POSTMORTEM;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_POSTMORTEM_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_PROPERTIES_RESOURCE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RAWQ_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RAWQ_SIZE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_OFF;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_REFINERY_SIZE_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RUN;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RUN_DEFAULT;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_RUN_FOLDER;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SEPARATE_STREAMS;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SITES_FILE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SITES_FILE_NAME;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SITES_RESOURCE;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SOURCE_FOLDER_NAME;
import static com.surelogic._flashlight.common.InstrumentationConstants.FL_SOURCE_RESOURCE;
import static com.surelogic._flashlight.common.InstrumentationConstants.JAVA_IO_TMPDIR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.surelogic._flashlight.common.CollectionType;
import com.surelogic._flashlight.common.OutputType;

/**
 * This class is giant hack, but I don't know of a better way to do things. This
 * class cannot be instantiated, but contains various configuration attributes
 * for the {@link Store} class. These attributes must be set <em>before</em> the
 * {@code FLStore} class is initialized by the virtual machine because the
 * attributes are accessed by the <code>static</code> initializer of the class.
 * 
 * <p>
 * This class initializes the attributes from Java System properties, but these
 * values can be overridden using the various setter methods.
 */
public class StoreConfiguration {

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
	private static volatile String dateOverride;
	private static volatile boolean isRefineryOff;
	private static volatile OutputType outputType;
	private static volatile CollectionType collectionType;
	private static volatile boolean handleFieldAccesses;
	private static volatile boolean useSeparateStreams;
	private static volatile boolean debug;
	private static volatile boolean isPostmortemMode;

	static {
		// System.out.println("StoreConfiguration");
		ClassLoader context = Thread.currentThread().getContextClassLoader();

		Properties props = System.getProperties();

		// We try to load properties from InstrumentationConf, if it exists.
		// This file is sometimes generated during the instrumentation
		// phase.
		try {
			props.setProperty(FL_COLLECTION_TYPE,
					InstrumentationConf.getFL_COLLECTION_TYPE());
			props.setProperty(FL_RUN, InstrumentationConf.getFL_RUN());
			props.setProperty(FL_RUN_FOLDER,
					InstrumentationConf.getFL_RUN_FOLDER());
		} catch (NoClassDefFoundError e) {
			// Do nothing
		}

		// We try to load properties from a special properties file, but we do
		// not override properties already set on the command line
		InputStream in = context.getResourceAsStream(FL_PROPERTIES_RESOURCE);
		if (in != null) {
			try {
				Properties newProps = new Properties();
				newProps.load(in);
				for (Entry<Object, Object> e : newProps.entrySet()) {
					if (!props.containsKey(e.getKey())) {
						props.put(e.getKey(), e.getValue());
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		// Initialize the settings base on Java System properties
		setOff(props.getProperty(FL_OFF, null) != null);
		setRun(props.getProperty(FL_RUN, FL_RUN_DEFAULT));

		setPostmortemMode(Boolean.parseBoolean(props.getProperty(FL_POSTMORTEM,
				FL_POSTMORTEM_DEFAULT)));
		// Check for a date override
		setDateOverride(props.getProperty(FL_DATE_OVERRIDE));

		// We use a specific directory if specified, otherwise we will make one
		// ourselves
		if (props.containsKey(FL_DIR)) {
			setDirectory(props.getProperty(FL_DIR));
		} else if (props.containsKey(FL_RUN_FOLDER)) {
			String d = getDateOverride();
			if (d == null) {
				DateFormat df = new SimpleDateFormat(DATE_FORMAT);
				// We will override the date ourselves, in order to make the
				// folder match up with the data
				d = df.format(new Date());
				setDateOverride(d);
			}
			File dir = new File(props.getProperty(FL_RUN_FOLDER), getRun() + d);
			dir.mkdir();
			// FIXME we set up the additional folders needed by eclipse here,
			// but we should probably remove the dependencies in eclipse or do
			// something more here
			new File(dir, "source").mkdir();
			new File(dir, "external").mkdir();
			new File(dir, "projects").mkdir();
			setDirectory(dir.getAbsolutePath());
		} else {
			setDirectory(System.getProperty(JAVA_IO_TMPDIR));
		}
		// We will use specific field and sites files, but otherwise we will
		// look to see if the files are embedded in the instrumented code. If
		// this is the case, we also copy the files into the run directory
		if (props.containsKey(FL_FIELDS_FILE)) {
			setFieldsFile(props.getProperty(FL_FIELDS_FILE));
		} else {
			InputStream resource = context
					.getResourceAsStream(FL_FIELDS_RESOURCE);
			if (resource != null) {
				File fieldsFile = new File(getDirectory(), FL_FIELDS_FILE_NAME);
				copy(resource, fieldsFile, true);
				setFieldsFile(fieldsFile.getAbsolutePath());
			}
		}
		if (props.containsKey(FL_SITES_FILE)) {
			setSitesFile(props.getProperty(FL_SITES_FILE, FL_SITES_FILE_NAME));
		} else {
			InputStream resource = context
					.getResourceAsStream(FL_SITES_RESOURCE);
			if (resource != null) {
				File sitesFile = new File(getDirectory(), FL_SITES_FILE_NAME);
				copy(resource, sitesFile, true);
				setSitesFile(sitesFile.getAbsolutePath());
			}
		}
		InputStream resource = context.getResourceAsStream(FL_LOG_RESOURCE);
		if (resource != null) {
			File logFile = new File(getDirectory(), FL_LOG_FILE_NAME);
			copy(resource, logFile, true);
		}
		InputStream sources = context.getResourceAsStream(FL_SOURCE_RESOURCE);
		if (sources != null) {
			ZipInputStream zf = new ZipInputStream(sources);
			File sourceFolder = new File(getDirectory(), FL_SOURCE_FOLDER_NAME);
			try {
				for (ZipEntry entry = zf.getNextEntry(); entry != null; entry = zf
						.getNextEntry()) {
					copy(zf, new File(sourceFolder, entry.getName()), false);
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		setRawQueueSize(getIntProperty(props, FL_RAWQ_SIZE,
				FL_RAWQ_SIZE_DEFAULT));
		setOutQueueSize(getIntProperty(props, FL_OUTQ_SIZE,
				FL_OUTQ_SIZE_DEFAULT));
		setRefinerySize(getIntProperty(props, FL_REFINERY_SIZE,
				FL_REFINERY_SIZE_DEFAULT));
		setNoSpy(props.getProperty(FL_NO_SPY) != null);
		setConsolePort(getIntProperty(props, FL_CONSOLE_PORT,
				FL_CONSOLE_PORT_DEFAULT));
		setRefineryOff(props.getProperty(FL_REFINERY_OFF, null) != null);
		setOutputType(OutputType.valueOf(props.getProperty(FL_OUTPUT_TYPE),
				FL_OUTPUT_TYPE_DEFAULT));
		setDebug("ON".equalsIgnoreCase(props.getProperty(FL_DEBUG, "OFF")));
		setCollectionType(CollectionType.valueOf(
				props.getProperty(FL_COLLECTION_TYPE),
				FL_COLLECTION_TYPE_DEFAULT));
		useSeparateStreams("ON".equalsIgnoreCase(props.getProperty(
				FL_SEPARATE_STREAMS, "OFF")));
	}

	private static int getIntProperty(final Properties props, final String key,
			int def) {
		try {
			String intString = props.getProperty(key);
			if (intString != null) {
				def = Integer.parseInt(intString);
			}
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
	 * This value is initialized from the Java system property
	 * <code>FL_OFF</code>: If the property has a value, then the
	 * instrumentation is turned off.
	 */
	public static boolean isOff() {
		return isOff;
	}

	/**
	 * Set whether the instrumentation is disabled.
	 * 
	 * @param flag
	 *            {@code true} if the instrumentation should be disabled.
	 */
	public static void setOff(final boolean flag) {
		isOff = flag;
	}

	/**
	 * Get the directory to write the data into.
	 * 
	 * <p>
	 * Thie value is initialized from the Java system property
	 * <code>FL_DIR</code> with the default value coming from the
	 * <code>java.io.tmpdir</code> property.
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
	 * This value is initialized from the Java system property
	 * <code>FL_RUN</code> with the default value of <code>"flashlight"</code>.
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
	 * refinery (which deals with garbage collection). Many threads input and
	 * one thread drains this queue.
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
	 * refinery (which deals with garbage collection). Many threads input and
	 * one thread drains this queue. The size is always at least 10.
	 */
	public static void setRawQueueSize(final int size) {
		rawQueueSize = Math.max(size, 10);
	}

	/**
	 * Get the size of the BlockingQueue between the refinery and the depository
	 * (which deals with output). One thread inputs and another thread drains
	 * this queue.
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
	 * (which deals with output). One thread inputs and another thread drains
	 * this queue. The size is always at least 10.
	 */
	public static void setOutQueueSize(final int size) {
		outQueueSize = Math.max(size, 10);
	}

	/**
	 * Get the size of the List used to cache events in the refinery. This is
	 * the number of events the refinery holds on to before it puts events in to
	 * the output queue. If the garbage collector notifies the store that an
	 * object is garbage collected, and that object is thread local, then all
	 * the events about that object that are held in the refinery can be
	 * discarded (and therefore not output).
	 * 
	 * <p>
	 * This value is initialized from the Java system property
	 * <code>FL_REFINERY_SIZE</code> with the default value of 5000.
	 */
	public static int getRefinerySize() {
		return refinerySize;
	}

	/**
	 * Set the size of the List used to cache events in the refinery. This is
	 * the number of events the refinery holds on to before it puts events in to
	 * the output queue. If the garbage collector notifies the store that an
	 * object is garbage collected, and that object is thread local, then all
	 * the events about that object that are held in the refinery can be
	 * discarded (and therefore not output). The size is always at least 100.
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
	 * This value is initialized from the Java system property
	 * <code>FL_NO_SPY</code>: If the property has a value, then the
	 * instrumentation is turned off.
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
	 * The port that a console can connect to to shutdown the instrumentation.
	 * If the port specified is used the program counts up...it tries the next
	 * 100 and then gives up.
	 * 
	 * <P>
	 * This value is initialized from the Java system property
	 * <code>FL_CONSOLE_PORT</code> with the default value of 43524.
	 */
	public static int getConsolePort() {
		return consolePort;
	}

	/**
	 * Set the initial value for the console port. If the port specified is used
	 * the program counts up...it tries the next 100 and then gives up. This
	 * value must be at least 1024.
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

	public static String getDateOverride() {
		return dateOverride;
	}

	public static void setDateOverride(final String date) {
		dateOverride = date;
	}

	public static boolean isRefineryOff() {
		return isRefineryOff;
	}

	public static void setRefineryOff(final boolean off) {
		isRefineryOff = off;
	}

	public static OutputType getOutputType() {
		return outputType;
	}

	public static void setOutputType(final OutputType type) {
		outputType = type;
	}

	public static boolean debugOn() {
		return debug;
	}

	public static void setDebug(final boolean dbg) {
		debug = dbg;
	}

	public static CollectionType getCollectionType() {
		return collectionType;
	}

	public static void setCollectionType(final CollectionType type) {
		collectionType = type;
		handleFieldAccesses = collectionType.processFieldAccesses();
	}

	public static boolean processFieldAccesses() {
		return handleFieldAccesses;
	}

	public static boolean useSeparateStreams() {
		return useSeparateStreams;
	}

	public static void useSeparateStreams(final boolean separate) {
		useSeparateStreams = separate;
	}

	public static boolean isPostmortemMode() {
		return isPostmortemMode;
	}

	public static void setPostmortemMode(final boolean isPostmortemMode) {
		StoreConfiguration.isPostmortemMode = isPostmortemMode;
	}

	/**
	 * Copies the contents of a {@link InputStream} to a file.
	 * 
	 * @param is
	 *            the stream to copy from
	 * @param to
	 *            the target file.
	 * @return {@code true} if and only if the copy is successful, {@code false}
	 *         otherwise.
	 */
	public static boolean copy(InputStream is, final File to,
			final boolean closeStream) {
		boolean success = true;
		try {
			OutputStream os = null;
			try {
				is = new BufferedInputStream(is, 8192);
				os = new FileOutputStream(to);

				final byte[] buf = new byte[8192];
				int num;
				while ((num = is.read(buf)) >= 0) {
					os.write(buf, 0, num);
				}
			} finally {
				if (is != null && closeStream) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			}
		} catch (final IOException e) {

			success = false;
		}
		return success;
	}
}
