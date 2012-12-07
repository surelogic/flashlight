package com.surelogic.flashlight.common.model;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.TimeUnit;
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
  static RunDirectory getFor(final File directory) {
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
      return null;
    }

    // Find the last .complete snapshot
    final Pair<File, Integer> pair = FlashlightFileUtility.getLatestCheckpointCompleteFileAndItsNumberWithin(directory);
    if (pair == null)
      return null;

    final RawFileHandles rawFileHandles = FlashlightFileUtility.getRawFileHandlesFor(directory, pair.second());

    // read the prefix from the first checkpoint file
    final File firstSnapshotFile = rawFileHandles.getFirstCheckpointFile();
    final CheckpointFilePrefix prefix = FlashlightFileUtility.getPrefixFor(firstSnapshotFile);
    if (!prefix.isWellFormed()) {
      // can't make sense of the header file
      return null;
    }

    final long durationNanos = FlashlightFileUtility.readDurationInNanosFrom(pair.first());

    final RunDescription run = RunDescription.getInstance(prefix);
    if (run == null)
      return null;

    /**
     * A sanity check to make sure that we aren't still running the instrumented
     * program and collecting data. We do this by checking if anything has been
     * recently modified in the run directory.
     */
    final boolean isStillCollectingData = FileUtility.anythingModifiedInTheLast(directory, 3, TimeUnit.SECONDS);
    if (isStillCollectingData)
      return null;

    return new RunDirectory(run, durationNanos, directory, source, rawFileHandles);
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
   * The run duration in nanoseconds.
   */
  private final long f_runDurationNanos;

  /**
   * The file handles for the profile data
   */
  @NonNull
  private final RawFileHandles f_rawFileHandles;

  private RunDirectory(@NonNull final RunDescription runDescription, final long runDurationNanos, @NonNull final File runDirHandle,
      @NonNull final SourceZipFileHandles sourceZipFileHandles, @NonNull final RawFileHandles rawFileHandles) {
    f_runDescription = runDescription;
    f_runDirHandle = runDirHandle;
    f_sourceZipFileHandles = sourceZipFileHandles;
    f_rawFileHandles = rawFileHandles;
    f_runDurationNanos = runDurationNanos;
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
   * Gets the duration of collection in nanoseconds.
   * 
   * @return the duration of collection in nanoseconds.
   */
  public long getCollectionDurationInNanos() {
    return f_runDurationNanos;
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
   * Checks if this run has been, or is being, prepared by seeing if the handle
   * returned from {@link #getPrepDbDirectoryHandle()} exists.
   * 
   * @return {@code true} if this run has been, or is being, prepared,
   *         {@code false} otherwise.
   */
  public boolean isPreparedOrIsBeingPrepared() {
    return getPrepDbDirectoryHandle().exists();
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
   * Gets a human readable size of the database directory within this run
   * directory.
   * 
   * @return a human readable size of the database directory within this run
   *         directory.
   */
  @NonNull
  public String getHumanReadableDatabaseSize() {
    return FileUtility.bytesToHumanReadableString(FileUtility.recursiveSizeInBytes(getPrepDbDirectoryHandle()));
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
