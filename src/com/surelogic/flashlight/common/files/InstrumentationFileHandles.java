package com.surelogic.flashlight.common.files;

import java.io.File;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Model object that holds the file handles for the instrumentation-related 
 * files in a per-run directory.
 * @see RunDirectory
 */
public final class InstrumentationFileHandles {
  /** The standard name for the instrumentation field database file. */
  public static final String FIELDS_FILE_NAME = "fields.txt";
  /** The standard name for the instrumentation sites field database file. */
  public static final String SITES_FILE_NAME = "sites.txt";
  /** The standard name for the instrumentation log file. */
  public static final String INSTRUMENTATION_LOG_FILE_NAME = "instrumentation.log";

  
  
  private final File fieldsFileHandle;
  private final File sitesFileHandle;
  private final File logFileHandle;

  
  
  /* Private: use the factory method */
  private InstrumentationFileHandles(
      final File fieldsFile, final File sitesFile, final File logFile) {
    fieldsFileHandle = fieldsFile;
    sitesFileHandle = sitesFile;
    logFileHandle = logFile;
  }
  
  
  
  
  /**
   * <i>Implementation Note:</i> This constructor scans the Flashlight data
   * directory.
   * 
   * @return The handles object. Return {@code null} if one of the files cannot
   *         be found.
   */
  /* Package private: Can only be created by a RunDirectory */
  static InstrumentationFileHandles getFor(final File runDir) {
    final File fieldsFile = new File(runDir, FIELDS_FILE_NAME);
    final File sitesFile = new File(runDir, SITES_FILE_NAME);
    final File logFile = new File(runDir, INSTRUMENTATION_LOG_FILE_NAME);
    
    boolean failed = false;
    if (!fieldsFile.exists()) {
      failed = true;
      SLLogger.getLogger().log(Level.WARNING,
          I18N.err(148, runDir.getAbsolutePath(), FIELDS_FILE_NAME));
    }
    if (!sitesFile.exists()) {
      failed = true;
      SLLogger.getLogger().log(Level.WARNING,
          I18N.err(149, runDir.getAbsolutePath(), SITES_FILE_NAME));
    }
    if (!logFile.exists()) {
      failed = true;
      SLLogger.getLogger().log(Level.WARNING,
          I18N.err(150, runDir.getAbsolutePath(), INSTRUMENTATION_LOG_FILE_NAME));
    }
    if (failed) {
      return null;
    } else {
      return new InstrumentationFileHandles(fieldsFile, sitesFile, logFile);
    }
  }
  
  
  
  public File getFieldsFile() {
    return fieldsFileHandle;
  }
  
  public File getSitesFile() {
    return sitesFileHandle;
  }
  
  public File getInstrumentationLogFile() {
    return logFileHandle;
  }
}
