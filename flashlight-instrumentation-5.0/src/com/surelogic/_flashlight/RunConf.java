package com.surelogic._flashlight;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.common.FieldDef;
import com.surelogic._flashlight.common.FieldDefs;
import com.surelogic._flashlight.common.InstrumentationConstants;

/**
 * Contains information about a configured store. Used for logging and keeping
 * other pieces of information that are not expected to change during a run.
 * This class is thread safe.
 * 
 * @author nathan
 * 
 */
public class RunConf {

	/**
	 * Non-null if Flashlight should log to the console, <code>null</code>
	 * otherwise.
	 */
	private final PrintWriter f_log;

	private static final String ENCODING = "UTF-8";

	public String getEncoding() {
		return ENCODING;
	}

	/**
	 * Flags if helpful debug information should be output to the console log.
	 * This flag generates a lot of output and should only be set to
	 * {@code true} for small test programs.
	 */
	public static final boolean DEBUG = false;

	public boolean isDebug() {
		return DEBUG;
	}

	/**
	 * Logs a message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 */
	public void log(final String msg) {
		if (f_log != null) {
			f_log.println("[Flashlight] " + msg);
		}
	}

	/**
	 * Logs a message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 */
	public void debug(final String msg) {
		if (f_log != null && isDebug()) {
			f_log.println("[Flashlight] " + msg);
		}
	}

	/**
	 * Tracks the number off problems reported by the store.
	 */
	private final AtomicLong f_problemCount;

	public long getProblemCount() {
		return f_problemCount.get();
	}

	/**
	 * Logs a problem message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 */
	public void logAProblem(final String msg) {
		logAProblem(msg, new Exception());
	}

	/**
	 * Logs a problem message if logging is enabled.
	 * 
	 * @param msg
	 *            the message to log.
	 * @param e
	 *            reported exception.
	 */
	public void logAProblem(final String msg, final Exception e) {
		f_problemCount.incrementAndGet();
		if (f_log != null) {
			/*
			 * It is an undocumented lock policy that PrintStream locks on
			 * itself. To make all of our output appear together on the console
			 * we follow this policy.
			 */
			synchronized (f_log) {
				f_log.println("[Flashlight] !PROBLEM! " + msg);
				e.printStackTrace(f_log);
			}
		}
	}

	/**
	 * Flush the log.
	 */
	public void logFlush() {
		if (f_log != null) {
			f_log.flush();
		}
	}

	/**
	 * Closes the log.
	 */
	public void logComplete() {
		if (f_log != null) {
			f_log.close();
		}
	}

	/**
	 * The string value of the <tt>FL_RUN</tt> property or <tt>"flashlight"</tt>
	 * if this property is not set.
	 */
	private final String f_run;

	/**
	 * Gets the string value of the <tt>FL_RUN</tt> property or
	 * <tt>"flashlight"</tt> if this property is not set.
	 * 
	 * @return the string value of the <tt>FL_RUN</tt> property or
	 *         <tt>"flashlight"</tt> if this property is not set.
	 */
	public String getRun() {
		return f_run;
	}

	private final long f_start_nano;

	private final Date f_startTime;

	/**
	 * The value of {@link System#nanoTime()} when we start collecting data.
	 */
	public long getStartNanoTime() {
		return f_start_nano;
	}

	/**
	 * The value of {@link System#currentTimeMillis()} when we start collecting
	 * data.
	 * 
	 * @return
	 */
	public Date getStartTime() {
		return f_startTime;
	}

	private final Map<String, Integer> f_defs;
	private final FieldDefs f_fieldDefs;

	public FieldDefs getFieldDefs() {
		return f_fieldDefs;
	}

	/**
	 * Return the id associated with the given field in the fields.txt file
	 * 
	 * @param clazz
	 *            the fully-qualified class name.
	 * @param field
	 * @return a positive integer, or -1 if the field is not found
	 */
	public int getFieldId(final String clazz, final String field) {
		Integer id = f_defs.get(clazz + '.' + field);
		return id == null ? -1 : id;
	}

	private final String f_filePrefix;

	/**
	 * This is the unique file name prefix at the start of .flog, .fl, and .flh
	 * files.
	 * 
	 * @return
	 */
	public String getFilePrefix() {
		return f_filePrefix;
	}

	/**
	 * The number of events a single log file should (roughly) contain.
	 * 
	 * @return
	 */
	public int getFileEventCount() {
		return 100000;
	}

	/**
	 * Whether or not to use the checkpointing, multi-file way of outputting
	 * instrumentation data
	 * 
	 * @return
	 */
	public boolean isMultiFileOutput() {
		return true;
	}

	public RunConf() {
		// still incremented even if logging is off.
		f_problemCount = new AtomicLong();
		final File flashlightDir = new File(StoreConfiguration.getDirectory());
		if (!flashlightDir.exists()) {
			if (!flashlightDir.mkdirs()) {
				throw new IllegalStateException(
						String.format(
								"Could not start Flashlight instrumentation: %s could not be created",
								flashlightDir.toString()));
			}
		}
		// ??? What to do if mkdirs() fails???
		final StringBuilder fileName = new StringBuilder();
		fileName.append(flashlightDir);
		fileName.append(System.getProperty("file.separator"));
		f_run = StoreConfiguration.getRun();
		fileName.append(f_run);

		/*
		 * Get the start time of the data collection. This time is embedded in
		 * the time event in the output as well as in the names of the data and
		 * log files. The time can be provided in the configuration parameter
		 * "date override" so that we use the same start time in the data file
		 * as we use in the name of the flashlight run directory as constructed
		 * by the executing IDE.
		 */
		final SimpleDateFormat dateFormat = new SimpleDateFormat(
				InstrumentationConstants.DATE_FORMAT);
		final String dateOverride = StoreConfiguration.getDateOverride();

		Date startTime;
		if (dateOverride == null) {
			/* No time is provided, use the current time */
			startTime = new Date();
		} else {
			/*
			 * We have an externally provided time. Try to parse it. If we
			 * cannot parse it, use the current time.
			 */
			try {
				startTime = dateFormat.parse(dateOverride);
			} catch (final ParseException e) {
				System.err.println("[Flashlight] couldn't parse date string \""
						+ dateOverride + "\"");
				startTime = new Date();
			}
		}
		f_startTime = startTime;
		fileName.append(dateFormat.format(f_startTime));

		f_filePrefix = fileName.toString();
		f_log = initLog(fileName.toString());

		f_start_nano = System.nanoTime();
		FieldDefs defs;
		try {
			defs = new FieldDefs(FieldsConf.getFieldLines());
		} catch (NoClassDefFoundError xxx) {
			try {
				String fieldsFile = StoreConfiguration.getFieldsFile();
				if (fieldsFile == null) {
					defs = new FieldDefs();
				} else {
					defs = new FieldDefs(fieldsFile);
				}
			} catch (IOException e) {
				logAProblem(e.getMessage(), e);
				throw new IllegalStateException(e);
			}
		}
		f_fieldDefs = defs;
		f_defs = initDefs();
	}

	private static PrintWriter initLog(final String fileName) {
		final File logFile = new File(fileName.toString() + ".flog");
		PrintWriter w = null;
		try {
			OutputStream stream = new FileOutputStream(logFile);
			stream = new BufferedOutputStream(stream);
			w = new PrintWriter(stream);
		} catch (final IOException e) {
			System.err.println("[Flashlight] unable to log to \""
					+ logFile.getAbsolutePath() + "\"");
			e.printStackTrace(System.err);
			System.exit(1); // bail
		}
		return w;
	}

	private HashMap<String, Integer> initDefs() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (Entry<Long, FieldDef> def : f_fieldDefs.entrySet()) {
			map.put(def.getValue().getQualifiedFieldName(), def.getKey()
					.intValue());
		}
		return map;
	}

}
