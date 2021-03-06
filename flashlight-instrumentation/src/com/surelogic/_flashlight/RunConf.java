package com.surelogic._flashlight;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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
 */
public class RunConf {

  /**
   * Non-null if Flashlight should log to the console, <code>null</code>
   * otherwise.
   */
  private final PrintStream f_log;

  /**
   * Flags if helpful debug information should be output to the console log.
   * This flag generates a lot of output and should only be set to {@code true}
   * for small test programs.
   */
  public static final boolean DEBUG = false;

  public boolean isDebug() {
    return DEBUG;
  }

  /**
   * Logs a message if logging is enabled.
   *
   * @param msg
   *          the message to log.
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
   *          the message to log.
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
   *          the message to log.
   */
  public void logAProblem(final String msg) {
    logAProblem(msg, new Exception());
  }

  /**
   * Logs a problem message if logging is enabled.
   *
   * @param msg
   *          the message to log.
   * @param e
   *          reported exception.
   */
  public void logAProblem(final String msg, final Exception e) {
    f_problemCount.incrementAndGet();
    if (f_log != null) {
      /*
       * It is an undocumented lock policy that PrintStream locks on itself. To
       * make all of our output appear together on the console we follow this
       * policy.
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
   *          the fully-qualified class name.
   * @param field
   * @return a positive integer, or -1 if the field is not found
   */
  public int getFieldId(final String clazz, final String field) {
    Integer id = f_defs.get(clazz + '.' + field);
    return id == null ? -1 : id;
  }

  /**
   * The number of events a single log file should (roughly) contain.
   *
   * @return
   */
  public int getFileEventCount() {
    return InstrumentationConstants.FILE_EVENT_COUNT;
  }

  /**
   * The maximum initial amount of time in milliseconds that should be in the
   * first file.
   */
  public long getFileEventInitialDuration() {
    return InstrumentationConstants.FILE_EVENT_INITIAL_DURATION;
  }

  /**
   * The amount of time per file.
   *
   * @return
   */
  public long getFileEventDuration() {
    return InstrumentationConstants.FILE_EVENT_DURATION;
  }

  public RunConf() {
    // still incremented even if logging is off.
    f_problemCount = new AtomicLong();
    Date startTime;
    final SimpleDateFormat dateFormat = new SimpleDateFormat(InstrumentationConstants.DATE_FORMAT);
    final String dateOverride = StoreConfiguration.getDateOverride();
    if (dateOverride == null) {
      /* No time is provided, use the current time */
      startTime = new Date();
    } else {
      /*
       * We have an externally provided time. Try to parse it. If we cannot
       * parse it, use the current time.
       */
      try {
        startTime = dateFormat.parse(dateOverride);
      } catch (final ParseException e) {
        System.err.println("[Flashlight] couldn't parse date string \"" + dateOverride + "\"");
        startTime = new Date();
      }
    }
    f_startTime = startTime;
    f_run = StoreConfiguration.getRun();
    if (StoreConfiguration.getDirectory() != null) {
      final File flashlightDir = new File(StoreConfiguration.getDirectory());
      if (!flashlightDir.exists()) {
        if (!flashlightDir.mkdirs()) {
          throw new IllegalStateException(
              String.format("Could not start Flashlight instrumentation: %s could not be created", flashlightDir.toString()));
        }
      }
      // Touch the port file to indicate that we are starting
      f_log = initLog(flashlightDir);
      try {
        new File(flashlightDir, InstrumentationConstants.FL_PORT_FILE_LOC).createNewFile();
      } catch (IOException e) {
        logAProblem("Could not create port file", e);
      }
    } else {
      f_log = initLog(null);
    }

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
          defs = new FieldDefs(new File(fieldsFile));
        }
      } catch (IOException e) {
        logAProblem(e.getMessage(), e);
        throw new IllegalStateException(e);
      }
    }
    f_fieldDefs = defs;
    f_defs = initDefs();
  }

  private static PrintStream initLog(File flashlightDir) {
    PrintStream w = System.err;
    if (flashlightDir != null) {
      final File logFile = new File(flashlightDir, InstrumentationConstants.FL_RUNTIME_LOG_LOC);
      try {
        OutputStream stream = new FileOutputStream(logFile);
        stream = new BufferedOutputStream(stream);
        w = new PrintStream(stream);
      } catch (final IOException e) {
        System.err.println("[Flashlight] unable to log to \"" + logFile.getAbsolutePath() + "\"");
        e.printStackTrace(System.err);
      }
    }
    return w;
  }

  private HashMap<String, Integer> initDefs() {
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    for (Entry<Long, FieldDef> def : f_fieldDefs.entrySet()) {
      map.put(def.getValue().getQualifiedFieldName(), def.getKey().intValue());
    }
    return map;
  }

}
