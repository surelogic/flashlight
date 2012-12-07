package com.surelogic.flashlight.common.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Holds file handles to the data file(s) and the log file of a Flashlight run
 * as well as time information.
 * 
 * @see FlashlightFileUtility
 */
public final class RawFileHandles {

  RawFileHandles(@NonNull final File[] data, @Nullable final File[] logs) {
    if (data == null)
      throw new IllegalArgumentException(I18N.err(44, "data"));
    if (data.length < 1)
      throw new IllegalArgumentException(I18N.err(92, "data"));

    f_data = new File[data.length];
    for (int j = 0; j < data.length; j++) {
      final File inData = data[j];
      if (inData == null)
        throw new IllegalArgumentException(I18N.err(44, "data[" + j + "]"));
      f_data[j] = inData;
    }
    /*
     * The log files can be null, but we store this as an empty array.
     */
    f_logs = logs == null ? new File[0] : logs;
  }

  /**
   * Will contain at least one element.
   */
  @NonNull
  private final File[] f_data;

  /**
   * Gets the reference to the snapshot data files ordered from 0 to <i>n</i>.
   * Do not mutate the returned array.
   * 
   * @return the reference to the snapshot data files ordered from 0 to
   *         <i>n</i>. May be empty.
   */
  @NonNull
  public File[] getDataFiles() {
    return f_data;
  }

  /**
   * Gets a copy of the list of snapshot data files ordered from 0 to <i>n</i>.
   * 
   * @return a copy of the list of snapshot data files ordered from 0 to
   *         <i>n</i>. May be empty.
   */
  @NonNull
  public ArrayList<File> getOrderedListOfDataFiles() {
    final ArrayList<File> result = new ArrayList<File>();
    for (File f : f_data)
      result.add(f);
    return result;
  }

  /**
   * Checks if the first known data file is compressed.
   * 
   * @return {@code true} if the data file is compressed, {@code false}
   *         otherwise.
   * 
   * @see FlashlightFileUtility#isRawFileGzip(File)
   */
  public boolean isDataFileGzip() {
    return FlashlightFileUtility.isRawFileGzip(f_data[0]);
  }

  @NonNull
  private final File[] f_logs;

  /**
   * Gets all the log files found in the Flashlight directory. Do not mutate the
   * returned array.
   * 
   * @return An array referencing all that log files found in the Flashlight
   *         directory. May be empty.
   */
  @NonNull
  public File[] getLogFiles() {
    return f_logs;
  }

  /**
   * Checks the log of this raw file and reports if it is clean. A log file is
   * clean if it doesn't contain the string <tt>!PROBLEM!</tt> within it.
   * <tt>!PROBLEM!</tt> is the special string that the instrumentation uses to
   * highlight a problem with.
   * 
   * @return {@code true} if the log file is clean or doesn't exist,
   *         {@code false} otherwise.
   */
  public boolean isLogClean() {
    boolean result = true;
    for (final File log : f_logs) {
      result &= isLogCleanHelper(log);
    }
    return result;
  }

  private boolean isLogCleanHelper(@NonNull File log) {
    try {
      if (log == null || !log.exists()) {
        return true;
      }
      final BufferedReader r = new BufferedReader(new FileReader(log));
      try {
        while (true) {
          final String s = r.readLine();
          if (s == null) {
            break;
          }
          if (s.indexOf("!PROBLEM!") != -1) {
            return false;
          }
        }
      } finally {
        r.close();
      }
    } catch (final Exception e) {
      SLLogger.getLogger().log(Level.SEVERE, I18N.err(40, log == null ? null : log.getAbsolutePath()), e);
    }
    return true;
  }

  public String getLogContentsAsAString() {
    final StringBuilder b = new StringBuilder();
    for (final File log : f_logs) {
      b.append("------------------------------------------------------\n");
      b.append("LOG FILE: ").append(log.getName()).append("\n\n");
      b.append("          ").append(log.getAbsolutePath()).append('\n');
      b.append("------------------------------------------------------\n\n");
      b.append(FileUtility.getFileContentsAsStringOrDefaultValue(log, "-empty-"));
      b.append("\n\n");
    }
    return b.toString();
  }
}
