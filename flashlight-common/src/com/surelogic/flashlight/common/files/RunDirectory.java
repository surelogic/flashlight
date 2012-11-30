package com.surelogic.flashlight.common.files;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.ReferenceObject;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.FlashlightDBConnection;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Model for manipulating a per-run flashlight data directory, including the
 * instrumentation artifacts, source zip files, jar files, and profile data.
 */
@ReferenceObject
public final class RunDirectory {

  /**
   * Scan the given directory and gather the file handles for the contained
   * structures, returns {@code null} if anything is missing or wrong.
   * 
   * @param directory
   *          a flashlight run directory.
   * @return The model object or {@code null} if one of the necessary files
   *         doesn't exist.
   */
  @Nullable
  static RunDirectory getFor(final File directory) {
    if (directory == null)
      throw new IllegalArgumentException(I18N.err(44, "directory"));
    if (!directory.exists())
      throw new IllegalArgumentException(I18N.err(95, directory.getAbsolutePath(), "it does not exist"));
    if (!directory.isDirectory())
      throw new IllegalArgumentException(I18N.err(95, directory.getAbsolutePath(), "it is not a directory"));

    final File invalidFile = new File(directory, InstrumentationConstants.FL_INVALID_RUN);
    if (invalidFile.exists()) {
      return null;
    }

    final InstrumentationFileHandles instrumentation = InstrumentationFileHandles.getFor(directory);
    if (instrumentation == null)
      return null;

    final SourceZipFileHandles source = SourceZipFileHandles.getFor(directory);
    if (source == null)
      return null;

    final ProjectsDirectoryHandles projects = ProjectsDirectoryHandles.getFor(directory);
    if (projects == null)
      return null;

    /*
     * This process relies on RawFileUtility because RawFileHandles is the
     * original file handle class, and everything else is being built around the
     * machinery that existed to compute them.
     */
    final File headerFile = getFileFrom(directory, flashlightHeaderFileFilter, 152, 153);
    if (headerFile == null) {
      // no header file exists
      return null;
    }

    final RawDataFilePrefix headerInfo = RawFileUtility.getPrefixFor(headerFile);
    if (!headerInfo.isWellFormed()) {
      // can't make sense of the header file
      return null;
    }

    /*
     * If we get here the profile data files are okay, now check that the other
     * files are okay too.
     */
    final File[] rawDataFiles = getFilesFrom(directory, flashlightRawDataFileFilter, 146, 147);
    if (rawDataFiles == null) {
      // no raw files are in the directory
      return null;
    }
    final List<RawDataFilePrefix> prefixInfos = new ArrayList<RawDataFilePrefix>(Arrays.asList(RawFileUtility
        .getPrefixesFor(rawDataFiles)));
    // Take out files if their prefix is not well formed
    for (final Iterator<RawDataFilePrefix> iter = prefixInfos.iterator(); iter.hasNext();) {
      if (!iter.next().isWellFormed()) {
        iter.remove();
      }
    }
    if (prefixInfos.isEmpty()) {
      // We don't have any valid data in this directory
      return null;
    }

    final RunDescription run = RawFileUtility.getRunDescriptionFor(headerInfo);
    if (run == null)
      return null;

    if (!run.isCompleted()) {
      /**
       * A sanity check to make sure that we aren't still running the
       * instrumented program and collecting data.
       */
      final long cur = System.currentTimeMillis();
      for (final File f : rawDataFiles) {
        if (cur - f.lastModified() < InstrumentationConstants.FILE_EVENT_DURATION) {
          return null;
        }
      }
    }
    final RawFileHandles rawFileHandles = RawFileUtility.getRawFileHandlesFor(directory,
        prefixInfos.toArray(new RawDataFilePrefix[prefixInfos.size()]));

    return new RunDirectory(run, directory, headerFile, instrumentation, source, projects, rawFileHandles);
  }

  @Nullable
  private static File getFileFrom(final File runDir, final FileFilter filter, final int noFileErr, final int manyFilesErr) {
    final File[] files = runDir.listFiles(filter);
    /*
     * files is either null, or should be a array of length 1. It should only be
     * null when we get here after a directory refresh has been kicked off after
     * a delete of a run directory.
     */
    if (files != null) {
      // Must have exactly one data file
      if (files.length == 0) {
        SLLogger.getLogger().log(Level.FINE, I18N.err(noFileErr, runDir.getAbsolutePath()));
      } else if (files.length > 1) {
        SLLogger.getLogger().log(Level.FINE, I18N.err(manyFilesErr, runDir.getAbsolutePath()));
      } else { // exactly 1 (because length cannot be < 0)
        return files[0];
      }
    }
    return null;
  }

  private static File[] getFilesFrom(final File runDir, final FileFilter filter, final int noFileErr, final int wrongNumFilesErr) {
    final File[] files = runDir.listFiles(filter);
    /*
     * files is either null, or should be a array of length >1. It should only
     * be null when we get here after a directory refresh has been kicked off
     * after a delete of a run directory.
     */
    if (files != null && files.length == 0) {
      SLLogger.getLogger().log(Level.FINE, I18N.err(noFileErr, runDir.getAbsolutePath()));
    }
    return files;
  }

  /*
   * String constants about the contents of a Flashlight run directory.
   */

  /** Name of the subdirectory that contains the database */
  private static final String DB_DIR = "db";
  /** Name of the empty queries file */
  private static final String QUERIES_FILE = "empty-queries.txt";

  /**
   * Filter used to identify files that may be raw flashlight data files.
   */
  private static final FileFilter flashlightRawDataFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File pathname) {
      if (pathname.isDirectory()) {
        return false;
      }
      return OutputType.mayBeRawDataFile(pathname);
    }
  };

  /**
   * Filter used to identify header files for raw flashlight data files.
   */
  private static final FileFilter flashlightHeaderFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File pathname) {
      if (pathname.isDirectory()) {
        return false;
      }
      final String name = pathname.getName();
      if (name.endsWith(OutputType.FLH.getSuffix())) {
        return true;
      }
      return false;
    }
  };

  /**
   * This directory.
   */
  @NonNull
  private final File f_runDirHandle;

  /**
   * The run header file
   */
  @NonNull
  private final File f_headerFileHandle;

  /**
   * A description of the run in this directory.
   */
  @NonNull
  private final RunDescription f_runDescription;

  /**
   * The file handles for the instrumentation files.
   */
  @NonNull
  private final InstrumentationFileHandles f_instrumentationFileHandles;

  /**
   * The file handles for the source zips.
   */
  @NonNull
  private final SourceZipFileHandles f_sourceZipFileHandles;

  /**
   * The file handles for the project jar files.
   */
  @NonNull
  private final ProjectsDirectoryHandles f_projectDirHandles;

  /**
   * The file handles for the profile data
   */
  @NonNull
  private final RawFileHandles f_rawFileHandles;

  private RunDirectory(@NonNull final RunDescription runDescription, @NonNull final File runDirHandle,
      @NonNull final File headerFileHandle, @NonNull final InstrumentationFileHandles instrumentationFileHandles,
      @NonNull final SourceZipFileHandles sourceZipFileHandles, @NonNull final ProjectsDirectoryHandles projectDirHandles,
      @NonNull final RawFileHandles rawFileHandles) {
    f_runDescription = runDescription;
    f_runDirHandle = runDirHandle;
    f_headerFileHandle = headerFileHandle;
    f_instrumentationFileHandles = instrumentationFileHandles;
    f_sourceZipFileHandles = sourceZipFileHandles;
    f_projectDirHandles = projectDirHandles;
    f_rawFileHandles = rawFileHandles;
  }

  /**
   * Gets a run description for the run in this.
   * 
   * @return a run description for the run in this.
   */
  @NonNull
  public RunDescription getRunDescription() {
    return f_runDescription;
  }

  /**
   * Gets an abstract representation of this run directory.
   * 
   * @return an abstract representation of this run directory.
   */
  @NonNull
  public File getRunDirectory() {
    return f_runDirHandle;
  }

  /**
   * Gets a human readable size of this run directory.
   * 
   * @return a human readable size of this run directory.
   */
  @NonNull
  public String getHumanReadableSize() {
    return FileUtility.bytesToHumanReadableString(FileUtility.recursiveSizeInBytes(f_runDirHandle));
  }

  /**
   * Gets an abstract representation of the header file in this run directory.
   * 
   * @return an abstract representation of the header file in this run
   *         directory.
   */
  @NonNull
  public File getHeaderFile() {
    return f_headerFileHandle;
  }

  /**
   * Get the handles for the instrumentation artifacts.
   * 
   * @return handles for the instrumentation artifacts.
   */
  @NonNull
  public InstrumentationFileHandles getInstrumentationFileHandles() {
    return f_instrumentationFileHandles;
  }

  /**
   * Get the handles for the source Zip files.
   * 
   * @return handles for the source Zip files.
   */
  @NonNull
  public SourceZipFileHandles getSourceZipFileHandles() {
    return f_sourceZipFileHandles;
  }

  /**
   * Get the handles for the project JAR files.
   * 
   * @return handles for the project JAR files.
   */
  @NonNull
  public ProjectsDirectoryHandles getProjectsDirectoryHandles() {
    return f_projectDirHandles;
  }

  /**
   * Get the handles for the raw data files.
   * 
   * @return handles for the raw data files.
   */
  @NonNull
  public RawFileHandles getRawFileHandles() {
    return f_rawFileHandles;
  }

  /**
   * Gets an abstract handle to the database directory for this run. This
   * directory may or may not exist depending upon if the run is prepared.
   * 
   * @return an abstract handle to the database directory for this run.
   */
  @NonNull
  public File getDatabaseDirectory() {
    final File db = new File(f_runDirHandle, DB_DIR);
    return db;
  }

  /**
   * Checks if this run has been, or is being, prepared by seeing if the handle
   * returned from {@link #getDatabaseDirectory()} exists.
   * 
   * @return {@code true} if this run has been, or is being, prepared,
   *         {@code false} otherwise.
   */
  public boolean isPreparedOrIsBeingPrepared() {
    return getDatabaseDirectory().exists();
  }

  /**
   * Gets a database connection for the database directory for this run.
   * 
   * @return a database connection for the database directory for this run.
   */
  public DBConnection getDB() {
    return FlashlightDBConnection.getInstance(getDatabaseDirectory());
  }

  /**
   * Gets a human readable size of the database directory within this run
   * directory.
   * 
   * @return a human readable size of the database directory within this run
   *         directory.
   */
  @NonNull
  public String getHumanReadableDatabaseSize() {
    return FileUtility.bytesToHumanReadableString(FileUtility.recursiveSizeInBytes(getDatabaseDirectory()));
  }

  /**
   * Gets the handles for the HTML directory containing an overview of the
   * findings for this run.
   * 
   * @return handles for the HTML directory containing an overview of the
   *         findings for this run.
   * 
   * @throws IllegalStateException
   *           if something goes wrong.
   */
  @NonNull
  public HtmlHandles getHtmlHandles() {
    final HtmlHandles result = HtmlHandles.getFor(f_runDirHandle);
    if (result == null)
      throw new IllegalStateException("Unable to work on html within " + f_runDirHandle.getAbsolutePath());
    else
      return result;
  }

  /**
   * Gets an abstract representation of the list of queries containing no data.
   * 
   * @return an abstract representation of the list of queries containing no
   *         data.
   */
  @NonNull
  public File getEmptyQueriesFile() {
    return new File(f_runDirHandle, QUERIES_FILE);
  }
}
