package com.surelogic.flashlight.common.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Utility;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * A utility designed to work with Flashlight data files and the contents of the
 * Flashlight data directory.
 */
@Utility
public final class RawFileUtility {

  /**
   * A heuristic used to check that we aren't still running the instrumented
   * program and collecting data into the passed directory. We do this by
   * checking if anything has been recently modified in the run directory.
   * 
   * @param directory
   *          a Flashlight run directory.
   * @return {@code true} if it appears the instrumented program is no longer
   *         collecting data into the passed directory, {@code true} if it still
   *         seems to be outputting data.
   */
  public static boolean doneCollectingDataInto(final File directory) {
    final boolean stillCollectingData = FileUtility.anythingModifiedInTheLast(directory, 3, TimeUnit.SECONDS);
    return !stillCollectingData;
  }

  /**
   * Finds the last ".complete" snapshot file in the passed directory and
   * returns it and its number as a pair.
   * 
   * @param directory
   *          a directory
   * @return the last ".complete" snapshot file in the passed directory and
   *         returns it and its number as a pair or {@code null} if no such file
   *         could be found.
   */
  @Nullable
  public static Pair<File, Integer> getLatestCheckpointCompleteFileAndItsNumberWithin(final File directory) {
    File lastCheckpoint = null;
    int lastCheckpointNum = -1;
    for (File complete : OutputType.COMPLETE.getFilesWithin(directory)) {
      final String name = complete.getName();
      if (name.length() > 15) { // ex: ...000015.complete
        String numStr = name.substring(name.length() - 15, name.length() - 9);
        int thisCheckpointNum = -1;
        try {
          thisCheckpointNum = Integer.parseInt(numStr);
        } catch (NumberFormatException ignore) {
          SLLogger.getLogger().log(Level.WARNING, "Unable to parse '" + numStr + "' from checkpoint .complete file: " + name,
              ignore);
        }
        if (thisCheckpointNum > lastCheckpointNum) {
          lastCheckpoint = complete;
          lastCheckpointNum = thisCheckpointNum;
        }
      }
    }
    if (lastCheckpoint != null)
      return new Pair<File, Integer>(lastCheckpoint, lastCheckpointNum);
    else
      return null;
  }

  /**
   * Reads the single line within a <tt>.complete</tt> file which should look
   * something like
   * 
   * <pre>
   * 47385738473 ns
   * </pre>
   * 
   * and represents the run duration up to that snapshot in nanoseconds.
   * <p>
   * If something goes wrong a log message is generated and 0 is returned.
   * 
   * @param checkpointComplete
   *          the file to read
   * @return the duration in the passed file, or 0 if something goes wrong.
   */
  public static long readDurationInNanosFrom(final File checkpointComplete) {
    long duration = 0;
    try {
      final BufferedReader r = new BufferedReader(new FileReader(checkpointComplete));
      try {
        String line = r.readLine().trim();
        final int unitsIndex = line.indexOf("ns");
        if (unitsIndex != -1)
          line = line.substring(0, unitsIndex).trim();
        duration = Long.parseLong(line);
      } finally {
        r.close();
      }
    } catch (Exception e) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(226, checkpointComplete.getAbsolutePath()), e);
    }
    return duration;
  }

  /**
   * Checks if the passed raw file is compressed or not. It does this by
   * checking the file suffix (i.e., nothing fancy is done).
   * 
   * @param rawDataFile
   *          a raw data file.
   * @return {@code true} if the passed raw file is compressed, {@code false}
   *         otherwise.
   */
  public static boolean isRawFileGzip(final File rawDataFile) {
    return OutputType.detectFileType(rawDataFile).isCompressed();

  }

  /**
   * Checks to see if the passed directory appears to be a valid run directory
   * based upon its name (i.e., nothing fancy is done).
   * 
   * @param directory
   *          any directory.
   * @return {@code true} if the passed directory appears to be a valid run
   *         directory based upon its name, {@code false} otherwise.
   */
  public static boolean isRunDirectory(final File directory) {
    return f_runDirectoryFilter.accept(directory.getParentFile(), directory.getName());
  }

  /**
   * Gets RunDirectories for all the raw data files found in the Flashlight data
   * directory.
   * <p>
   * <i>Implementation Note:</i> This method scans the Flashlight data
   * directory.
   * 
   * @return a set of RunDirectories for all the raw data files found in the
   *         Flashlight data directory.
   */
  public static Collection<RunDirectory> getRunDirectories(final File dataDir) {
    final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader(dataDir);
    runDescriptionBuilder.read();
    return runDescriptionBuilder.getRunDirectories();
  }

  /**
   * Reads the prefix information from a raw data file.
   * 
   * @param rawDataFile
   *          a raw data file.
   * @return prefix information (may or may not be well-formed).
   */
  public static RawDataFilePrefix getPrefixFor(final File rawDataFile) {
    if (rawDataFile == null) {
      throw new IllegalArgumentException(I18N.err(44, "dataFile"));
    }

    final RawDataFilePrefix prefixInfo = new RawDataFilePrefix();
    prefixInfo.read(rawDataFile);

    return prefixInfo;
  }

  /**
   * Obtains the corresponding run description for the passed raw file prefix or
   * throws an exception.
   * 
   * @param prefixInfo
   *          a well-formed raw data file prefix.
   * @param durationNS
   *          run duration in Nanoseconds.
   * @return a run description based upon the passed prefix info.
   * @throws Exception
   *           if something goes wrong.
   */
  @NonNull
  public static RunDescription getRunDescriptionFor(final RawDataFilePrefix prefixInfo, final long durationNanos) {
    if (prefixInfo == null)
      throw new IllegalArgumentException(I18N.err(44, "prefixInfo"));

    if (prefixInfo.isWellFormed()) {
      return new RunDescription(prefixInfo.getName(), prefixInfo.getRawDataVersion(), prefixInfo.getHostname(),
          prefixInfo.getUserName(), prefixInfo.getJavaVersion(), prefixInfo.getJavaVendor(), prefixInfo.getOSName(),
          prefixInfo.getOSArch(), prefixInfo.getOSVersion(), prefixInfo.getMaxMemoryMb(), prefixInfo.getProcessors(),
          new Timestamp(prefixInfo.getWallClockTime().getTime()), durationNanos, prefixInfo.isAndroid());
    } else {
      throw new IllegalStateException(I18N.err(107, prefixInfo.getFile().getAbsolutePath()));
    }
  }

  /**
   * Obtains the corresponding raw file handles for the passed raw file prefixes
   * or throws an exception.
   * 
   * @param directory
   *          a run directory.
   * @param lastCheckpointCompleteFileNumber
   *          the number from the last ".complete" file in the directory,
   *          obtained by calling
   *          {@link #getLatestCheckpointCompleteFileAndItsNumberWithin(File)}.
   * @return a raw file handles.
   * 
   * @throws Exception
   *           if something goes wrong.
   */
  @NonNull
  public static RawFileHandles getRawFileHandlesFor(final File directory, final int lastCheckpointCompleteFileNumber) {
    /**
     * Filter used to identify files that may be raw flashlight data files.
     */
    final FileFilter flashlightRawDataFileFilter = new FileFilter() {
      @Override
      public boolean accept(final File pathname) {
        if (pathname.isDirectory()) {
          return false;
        }
        return OutputType.mayBeRawDataFile(pathname);
      }
    };
    final ArrayList<File> rawFiles = new ArrayList<File>();
    for (File raw : directory.listFiles(flashlightRawDataFileFilter))
      rawFiles.add(raw);

    final File[] orderedRawFiles = new File[lastCheckpointCompleteFileNumber + 1];
    for (int i = 0; i <= lastCheckpointCompleteFileNumber; i++) {
      File rawThisNum = null;
      for (Iterator<File> iterator = rawFiles.iterator(); iterator.hasNext();) {
        final File raw = iterator.next();
        final int fileNum = getDataFileNumHelper(raw);
        if (fileNum == i) {
          rawThisNum = raw;
          iterator.remove();
        }
      }
      if (rawThisNum != null)
        orderedRawFiles[i] = rawThisNum;
      else
        throw new IllegalStateException(I18N.err(108, i, directory.getAbsolutePath()));
    }

    final RawFileHandles handles = new RawFileHandles(orderedRawFiles, OutputType.LOG.getFilesWithin(directory));
    return handles;
  }

  private static int getDataFileNumHelper(@NonNull final File file) {
    int result = -1;
    if (file == null)
      return result;

    final String name = file.getName();
    if (name == null)
      return result;

    final OutputType type = OutputType.detectFileType(file);
    if (type == null || !OutputType.RAW_DATA.contains(type))
      return result;

    // remove extension
    int endIndex = name.length() - type.getSuffix().length();
    int beginIndex = endIndex - 6;
    final String numStr = name.substring(beginIndex, endIndex);

    try {
      result = Integer.parseInt(numStr);
    } catch (NumberFormatException ignore) {
      SLLogger.getLogger().log(Level.WARNING, "Unable to parse '" + numStr + "' from raw data file: " + name, ignore);
    }

    return result;
  }

  /**
   * Used to get all the run descriptions and file handles in the Flashlight
   * data directory.
   */
  private static final class RawDataDirectoryReader {

    @NonNull
    private final Map<RunDescription, RunDirectory> f_runToHandles = new HashMap<RunDescription, RunDirectory>();

    @NonNull
    private final File f_dataDir;

    RawDataDirectoryReader(final File dataDir) {
      if (dataDir == null) {
        throw new IllegalArgumentException(I18N.err(44, "dataDir"));
      }
      f_dataDir = dataDir;
    }

    @NonNull
    Collection<RunDirectory> getRunDirectories() {
      return f_runToHandles.values();
    }

    @Nullable
    RunDirectory getRunDirectoryFor(final RunDescription description) {
      return f_runToHandles.get(description);
    }

    @NonNull
    File[] getRunDirs() {
      final File[] runDirs = f_dataDir.listFiles(f_runDirectoryFilter);
      if (runDirs == null) {
        return new File[0];
      } else {
        return runDirs;
      }
    }

    void read() {
      for (final File runDir : getRunDirs()) {
        final RunDirectory runDirectory = RunDirectory.getFor(runDir);
        if (runDirectory != null) {
          final RunDescription run = runDirectory.getDescription();
          f_runToHandles.put(run, runDirectory);
        }
      }
    }
  }

  private static final FilenameFilter f_runDirectoryFilter = new FilenameFilter() {
    @Override
    public boolean accept(final File root, final String name) {
      final File dir = new File(root, name);
      return dir.isDirectory() && new File(dir, name + OutputType.FLH.getSuffix()).exists();
    }
  };

  /*
   * Estimate the amount of events in the raw file based upon the size of the
   * raw file. This guess is only used for the pre-scan of the file.
   */
  public static int estimateNumEvents(final File dataFile) {
    final long sizeInBytes = dataFile.length();
    long estimatedEvents = sizeInBytes / (RawFileUtility.isRawFileGzip(dataFile) ? 7L : 130L);
    if (estimatedEvents <= 0) {
      estimatedEvents = 10L;
    }
    return SLUtility.safeLongToInt(estimatedEvents);
  }

  private RawFileUtility() {
    // no instances
  }
}
