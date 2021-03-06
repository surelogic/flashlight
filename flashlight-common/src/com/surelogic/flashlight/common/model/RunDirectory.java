package com.surelogic.flashlight.common.model;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.ReferenceObject;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.logging.SLLogger;

/**
 * Model for manipulating a per-run flashlight data directory after data
 * collection has completed, including the instrumentation artifacts, source zip
 * files, jar files, and profile data.
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
  static RunDirectory getFor(final File directory, boolean debug) {
    if (directory == null) {
      throw new IllegalArgumentException(I18N.err(44, "directory"));
    }
    if (!directory.exists()) {
      throw new IllegalArgumentException(I18N.err(95, directory.getAbsolutePath(), "it does not exist"));
    }
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException(I18N.err(95, directory.getAbsolutePath(), "it is not a directory"));
    }

    final SourceZipFileHandles source = SourceZipFileHandles.getFor(directory);
    if (source == null) {
      if (debug)
        System.out.println("getFor(" + directory.getName() + ") failed: SourceZipFileHandles.getFor(directory)==null");
      return null;
    }

    // Find the last .complete snapshot
    final Pair<File, Integer> pair = FlashlightFileUtility.getLatestCheckpointCompleteFileAndItsNumberWithin(directory);
    if (pair == null) {
      if (debug)
        System.out.println("getFor(" + directory.getName()
            + ") failed: FlashlightFileUtility.getLatestCheckpointCompleteFileAndItsNumberWithin(directory)==null");

      return null;
    }

    final RawFileHandles rawFileHandles = FlashlightFileUtility.getRawFileHandlesFor(directory, pair.second());

    // read the prefix from the first checkpoint file
    final File firstSnapshotFile = rawFileHandles.getFirstCheckpointFile();
    final CheckpointFilePrefix prefix = FlashlightFileUtility.getPrefixFor(firstSnapshotFile);
    if (!prefix.isWellFormed()) {
      if (debug)
        System.out.println("getFor(" + directory.getName() + ") failed: !prefix.isWellFormed()");
      return null;
    }

    final long collectionDurationNanos = FlashlightFileUtility.readDurationInNanosFrom(pair.first());

    final RunDescription run = RunDescription.getInstance(prefix, collectionDurationNanos);
    if (run == null) {
      if (debug)
        System.out.println("getFor(" + directory.getName() + ") failed: RunDescription.getInstance(prefix)==null");
      return null;
    }

    return new RunDirectory(run, directory, source, rawFileHandles);
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

  /**
   * This directory.
   */
  @NonNull
  private final File f_runDirHandle;

  /**
   * A description of the run in this directory.
   */
  @NonNull
  private final RunDescription f_runDescription;

  /**
   * The file handles for the source zips.
   */
  @NonNull
  private final SourceZipFileHandles f_sourceZipFileHandles;

  /**
   * The file handles for the profile data
   */
  @NonNull
  private final RawFileHandles f_rawFileHandles;

  private RunDirectory(@NonNull final RunDescription runDescription, @NonNull final File runDirHandle,
      @NonNull final SourceZipFileHandles sourceZipFileHandles, @NonNull final RawFileHandles rawFileHandles) {
    f_runDescription = runDescription;
    f_runDirHandle = runDirHandle;
    f_sourceZipFileHandles = sourceZipFileHandles;
    f_rawFileHandles = rawFileHandles;
  }

  /**
   * Returns the name of the run directory denoted by this&mdash;sometime used
   * as a run identifier. This is just the last name in the run directory
   * pathname's name sequence (i.e., {@link #getDirectory()}<tt>.getName()</tt>
   * ).
   * <p>
   * For example, for a run directory for the PlanetBaron server, this call
   * returns
   * 
   * <pre>
   * com.surelogic.jsure.planetbaron.server.Server-2012.12.07-at-12.14.38.738
   * </pre>
   * 
   * This string is useful as an identifier for a Flashlight run. It should be
   * unique across runs due to the time at the end of the run directory name.
   * 
   * @return the name of the run directory denoted by this.
   * 
   * @see #getRunIdString()
   */
  @NonNull
  public String getSimpleRunDirectoryName() {
    return f_runDirHandle.getName();
  }

  /**
   * Gets a run identity string for this run. This method simply invokes
   * {@link #getSimpleRunDirectoryName()} and returns the result.
   * <p>
   * For example, for a run directory for the PlanetBaron server, this call
   * returns
   * 
   * <pre>
   * com.surelogic.jsure.planetbaron.server.Server-2012.12.07-at-12.14.38.738
   * </pre>
   * 
   * This string is useful as an identifier for a Flashlight run. It should be
   * unique across runs due to the time at the end of the run directory name.
   * 
   * @return as an identifier for a Flashlight run.
   */
  @NonNull
  public String getRunIdString() {
    return getSimpleRunDirectoryName();
  }

  /**
   * Gets if this run is from an instrumented Android application. If not, it is
   * is from an instrumented standard Java program.
   * 
   * @return {@code true} if this run is from an instrumented Android
   *         application, {@code false} if this run is from an instrumented
   *         standard Java program.
   */
  public boolean isAndroid() {
    return FlashlightFileUtility.isAndroid(getRunIdString());
  }

  /**
   * Constructs a name for the data preparation job of this run directory. We
   * need to know what this name is so we can find the prep job if we need to.
   * 
   * @return a name for the data preparation job of this run directory.
   */
  @NonNull
  public String getPrepJobName() {
    return "Preparing " + getRunIdString();
  }

  /**
   * Gets a run description for the run in this.
   * 
   * @return a run description for the run in this.
   */
  @NonNull
  public RunDescription getDescription() {
    return f_runDescription;
  }

  /**
   * Gets an abstract representation of this run directory.
   * 
   * @return an abstract representation of this run directory.
   */
  @NonNull
  public File getDirectory() {
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
   * Get the handles for the source Zip files.
   * 
   * @return handles for the source Zip files.
   */
  @NonNull
  public SourceZipFileHandles getSourceZipFileHandles() {
    return f_sourceZipFileHandles;
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
   * Constructs and returns an abstract representation of the database directory
   * within the prepared data directory.
   * <p>
   * This method does not create the directory or check if it actually exists.
   * 
   * @return an abstract representation of the database directory within the
   *         prepared data directory.
   */
  @NonNull
  public File getPrepDbDirectoryHandle() {
    return FlashlightFileUtility.getPrepDbDirectoryHandle(f_runDirHandle);
  }

  /**
   * Checks if this run has been prepared and is ready to be queried.
   * 
   * @return {@code true} if this run has been prepared and is ready to be
   *         queried, {@code false} otherwise.
   */
  public boolean isPrepared() {
    return FlashlightFileUtility.getPrepCompleteFileHandle(f_runDirHandle).exists();
  }

  /**
   * Gets a database connection for the database directory for this run.
   * 
   * @return a database connection for the database directory for this run.
   */
  public DBConnection getDB() {
    return FlashlightDBConnection.getInstance(getPrepDbDirectoryHandle());
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
    if (result == null) {
      throw new IllegalStateException("Unable to work on html within " + f_runDirHandle.getAbsolutePath());
    } else {
      return result;
    }
  }

  /**
   * Constructs and returns an abstract representation of the empty queries file
   * within the prepared data directory.
   * <p>
   * This method does not create the directory or check if it actually exists.
   * 
   * @return an abstract representation of the empty queries file within the
   *         prepared data directory.
   */
  @NonNull
  public File getPrepEmptyQueriesFileHandle() {
    return FlashlightFileUtility.getPrepEmptyQueriesFileHandle(f_runDirHandle);
  }

  /**
   * Constructs and returns an abstract representation of the prepared data
   * directory.
   * <p>
   * This method does not create the directory or check if it actually exists.
   * 
   * @return an abstract representation of the prepared data directory.
   */
  @NonNull
  public File getPrepDirectoryHandle() {
    return FlashlightFileUtility.getPrepDirectoryHandle(f_runDirHandle);
  }
}
