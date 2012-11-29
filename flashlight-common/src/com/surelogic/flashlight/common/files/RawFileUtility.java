package com.surelogic.flashlight.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.Utility;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * A utility designed to work with Flashlight data files and the contents of the
 * Flashlight data directory.
 */
@Utility
public final class RawFileUtility {
  public static final String DB_DIRECTORY = "db";

  /**
   * Checks if the passed raw file is compressed or not. It does this by
   * checking the file suffix (i.e., nothing fancy is done).
   * 
   * @param dataFile
   *          a raw data file.
   * @return {@code true} if the passed raw file is compressed, {@code false}
   *         otherwise.
   */
  public static boolean isRawFileGzip(final File dataFile) {
    return OutputType.detectFileType(dataFile).isCompressed();

  }

  /**
   * Checks to see if this folder is a valid run directory.
   * 
   * @param runDirectory
   * @return
   */
  public static boolean isRunDirectory(final File runDirectory) {
    return f_runDirectoryFilter.accept(runDirectory.getParentFile(), runDirectory.getName());
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

  public static List<File> findInvalidRunDirectories(final File dataDir) {
    final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader(dataDir);
    return runDescriptionBuilder.findBadDirs();
  }

  public static RunDirectory getRunDirectoryFor(final File dataDir, final RunDescription description) {
    if (description == null) {
      throw new IllegalArgumentException(I18N.err(44, "description"));
    }
    final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader(dataDir);
    runDescriptionBuilder.read();
    return runDescriptionBuilder.getRunDirectoryFor(description);
  }

  /**
   * Reads the prefix information from a raw data file.
   * 
   * @param dataFile
   *          a raw data file.
   * @return prefix information (may or may not be well-formed).
   */
  public static RawDataFilePrefix getPrefixFor(final File dataFile) {
    if (dataFile == null) {
      throw new IllegalArgumentException(I18N.err(44, "dataFile"));
    }
    final RawDataFilePrefix prefixInfo = new RawDataFilePrefix();
    prefixInfo.read(dataFile);

    return prefixInfo;
  }

  public static RawDataFilePrefix[] getPrefixesFor(final File[] dataFiles) {
    if (dataFiles == null) {
      throw new IllegalArgumentException(I18N.err(44, "dataFiles"));
    }
    for (File f : dataFiles) {
      if (f == null) {
        throw new IllegalArgumentException(I18N.err(44, "dataFiles"));
      }
    }
    RawDataFilePrefix[] rv = new RawDataFilePrefix[dataFiles.length];
    for (int i = 0; i < dataFiles.length; i++) {
      rv[i] = getPrefixFor(dataFiles[i]);
    }
    return rv;
  }

  /**
   * Obtains the corresponding run description for the passed raw file prefix.
   * 
   * @param prefixInfo
   *          a well-formed raw file prefix.
   * @return a run description.
   * @throws IllegalStateException
   *           if the prefix is not well-formed.
   * @throws IllegalArgumentException
   *           if the prefix is {@code null}.
   */
  public static RunDescription getRunDescriptionFor(final RawDataFilePrefix prefixInfo) {
    if (prefixInfo == null) {
      throw new IllegalArgumentException(I18N.err(44, "prefixInfo"));
    }

    if (prefixInfo.isWellFormed()) {

      final File runComplete = new File(prefixInfo.getFile().getParentFile(), InstrumentationConstants.FL_COMPLETE_RUN);
      long duration = 0;
      boolean completed = runComplete.exists();
      if (completed) {
        try {
          BufferedReader r = new BufferedReader(new FileReader(runComplete));
          try {
            duration = Long.parseLong(r.readLine());
          } finally {
            r.close();
          }
        } catch (NumberFormatException e) {
          // We are okay with this for now, since it can happen on old
          // runs that are otherwise valid
        } catch (IOException e) {
          SLLogger.getLogger().log(Level.WARNING, I18N.err(226, runComplete.getAbsolutePath()), e);
        }
      }

      return new RunDescription(prefixInfo.getName(), prefixInfo.getRawDataVersion(), prefixInfo.getHostname(),
          prefixInfo.getUserName(), prefixInfo.getJavaVersion(), prefixInfo.getJavaVendor(), prefixInfo.getOSName(),
          prefixInfo.getOSArch(), prefixInfo.getOSVersion(), prefixInfo.getMaxMemoryMb(), prefixInfo.getProcessors(),
          new Timestamp(prefixInfo.getWallClockTime().getTime()), duration, false /* TODO */, runComplete.exists());

    } else {
      throw new IllegalStateException(I18N.err(107, prefixInfo.getFile().getAbsolutePath()));
    }
  }

  /**
   * Obtains the corresponding raw file handles for the passed raw file prefix.
   * 
   * @param prefixInfo
   *          a well-formed raw file prefix.
   * @return the corresponding raw file handles for the passed raw file prefix.
   * @throws IllegalStateException
   *           if the prefix is not well-formed.
   * @throws IllegalArgumentException
   *           if the prefix is {@code null}.
   */
  public static RawFileHandles getRawFileHandlesFor(final File runDir, final RawDataFilePrefix[] prefixInfos) {
    if (prefixInfos == null) {
      throw new IllegalArgumentException(I18N.err(44, "prefixInfos"));
    }
    boolean wellFormed = true;
    for (RawDataFilePrefix p : prefixInfos) {
      if (p == null) {
        throw new IllegalArgumentException(I18N.err(44, "prefixInfos"));
      } else if (!p.isWellFormed()) {
        wellFormed = false;
      }
    }

    if (wellFormed) {
      // Find log file
      final File[] logs = runDir.listFiles(new LogFilter());
      final File logFile;
      if (logs == null || logs.length != 1) {
        SLLogger.getLogger().log(Level.FINE, I18N.err(108, prefixInfos[0].getFile().getAbsolutePath()));
        logFile = null;
      } else {
        logFile = logs[0];

        // Remove ".flog"
        final String fileNamePrefix = logFile.getName().substring(0, logFile.getName().length() - 5);
        for (RawDataFilePrefix p : prefixInfos) {
          if (!p.getFile().getName().startsWith(fileNamePrefix)) {
            SLLogger.getLogger().log(Level.WARNING, "Log name " + fileNamePrefix + " doesn't match data: " + p.getFile().getName());
          }
        }
      }
      final RawFileHandles handles = new RawFileHandles(prefixInfos, logFile);
      return handles;
    } else {
      throw new IllegalStateException(I18N.err(107, prefixInfos[0].getFile().getAbsolutePath()));
    }
  }

  private static final class LogFilter implements FilenameFilter {
    @Override
    public boolean accept(final File dir, final String name) {
      return name.endsWith(".flog");
    }
  }

  /**
   * Used to get all the run descriptions and file handles in the Flashlight
   * data directory.
   */
  private static final class RawDataDirectoryReader {
    private final Map<RunDescription, RunDirectory> f_runToHandles = new HashMap<RunDescription, RunDirectory>();

    private final File dataDir;

    RawDataDirectoryReader(final File dataDir) {
      this.dataDir = dataDir;
    }

    Collection<RunDirectory> getRunDirectories() {
      return f_runToHandles.values();
    }

    RunDirectory getRunDirectoryFor(final RunDescription description) {
      return f_runToHandles.get(description);
    }

    private File[] getRunDirs() {
      final File[] runDirs = dataDir.listFiles(f_runDirectoryFilter);
      return runDirs;
    }

    void read() {
      for (final File runDir : getRunDirs()) {
        final RunDirectory runDirectory = RunDirectory.getFor(runDir);
        if (runDirectory != null) {
          final RunDescription run = runDirectory.getRunDescription();
          f_runToHandles.put(run, runDirectory);
        }
      }
    }

    List<File> findBadDirs() {
      List<File> bad = new ArrayList<File>();
      for (final File runDir : getRunDirs()) {
        final RunDirectory runDirectory = RunDirectory.getFor(runDir);
        if (runDirectory == null) {
          bad.add(runDir);
        }
      }
      return bad;
    }
  }

  private static final FilenameFilter f_runDirectoryFilter = new FilenameFilter() {
    @Override
    public boolean accept(final File root, final String name) {
      final File dir = new File(root, name);
      return dir.exists() && dir.isDirectory() && new File(dir, name + OutputType.FLH.getSuffix()).exists();
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
