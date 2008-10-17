package com.surelogic.flashlight.common.files;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Model for manipulating a per-run flashlight data directory, including
 * the instrumentation artifacts, source zip files, jar files, and 
 * profile data.
 */
public final class RunDirectory {
  /** Suffix for a textual data file */
  public static final String SUFFIX = ".fl";
  /** Suffixed for a compressed textual data file */
  public static final String COMPRESSED_SUFFIX = ".fl.gz";
  /** Suffix for a binary data file */
  public static final String BIN_SUFFIX = ".flb";
  /** Suffix for a compressed binary data file */
  public static final String COMPRESSED_BIN_SUFFIX = ".flb.gz";
  
  /** Complete list of suffixes used to identify raw data files */
  private static final String[] suffixes = {
    COMPRESSED_SUFFIX, BIN_SUFFIX, SUFFIX, COMPRESSED_BIN_SUFFIX
  };

  public static final String HEADER_SUFFIX = ".flh";

  /**
   * Filter used to identify files that may be raw flashlight data files.
   */
  private static final FileFilter flashlightRawDataFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
      if (pathname.isDirectory()) {
        return false;
      }
      final String name = pathname.getName();     
      for(String suffix : suffixes) {
        if (name.endsWith(suffix)) {
          return true;
        }
      }
      return false;
    }
  };
  
  /**
   * Filter used to identify header files for raw flashlight data files.
   */
  private static final FileFilter flashlightHeaderFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
      if (pathname.isDirectory()) {
        return false;
      }
      final String name = pathname.getName();     
      if (name.endsWith(HEADER_SUFFIX)) {
          return true;
      }      
      return false;
    }
  };


  
  /** The run description for the data file contained in this directory */
  private final RunDescription runDescription;
  
  /** The file handle for the run directory itself. */
  private final File runDirHandle;
  
  /** The file handle for the header file */
  private final File headerHandle;
  
  /** The file handles for the instrumentation files. */
  private final InstrumentationFileHandles instrumentationFileHandles;
  /** The file handles for the source zips. */
  private final SourceZipFileHandles sourceZipFileHandles;
  /** The file handles for the project jar files */
  private final ProjectsDirectoryHandles projectDirHandles;
  /** The file handles for the profile data */
  private final RawFileHandles rawFileHandles;
  
  
  
  /* Private constructor: use the factory method */
  private RunDirectory(final RunDescription run, final File runDir,
      final File header, final InstrumentationFileHandles instrumentation,
      final SourceZipFileHandles source,
      final ProjectsDirectoryHandles projects, final RawFileHandles profile) {
    runDescription = run;
    headerHandle = header;
    runDirHandle = runDir;
    instrumentationFileHandles = instrumentation;
    sourceZipFileHandles = source;
    projectDirHandles = projects;
    rawFileHandles = profile;
  }
  
  
  
  /**
   * Scan the given directory and gather the file handles for the contained
   * structures.
   * <p>
   * <i>Implementation Note:</i> This constructor scans the Flashlight data
   * directory.
   * 
   * @param runDir
   *          Pathname to a per-run flashlight data directory.
   * @return The model object or {@code null} if one of the necessary files
   *         doesn't exist.
   */
  /* Package private: Only created by RawFileUtility */
  static RunDirectory getFor(final File runDir) {
    assert runDir != null; // Caller checks if this null
    final InstrumentationFileHandles instrumentation =
      InstrumentationFileHandles.getFor(runDir);
    final SourceZipFileHandles source = SourceZipFileHandles.getFor(runDir);
    final ProjectsDirectoryHandles projects = ProjectsDirectoryHandles.getFor(runDir);
    
    /* This process relies on RawFileUtility because RawFileHandles is the
     * original file handle class, and everything else is being built around the
     * machinery that preexisted to compute them.
     */
    final File headerFile = getFileFrom(runDir, flashlightHeaderFileFilter, 152, 153);
    if (headerFile != null) {
      final RawDataFilePrefix headerInfo = RawFileUtility.getPrefixFor(headerFile);
      if (headerInfo.isWellFormed()) {
        /* If we get here the profile data files are okay, now check that
         * the other files are okay too.
         */ 
        final File rawDataFile =
          getFileFrom(runDir, flashlightRawDataFileFilter, 146, 147);
        final RawDataFilePrefix prefixInfo =
          rawDataFile != null ? RawFileUtility.getPrefixFor(rawDataFile) : null;

        if (instrumentation != null && source != null && projects != null) {
          /* These calls won't fail because we know that prefixInfo is non-null
           * and well formed.
           */ 
          final RunDescription run = RawFileUtility.getRunDescriptionFor(headerInfo);
          final RawFileHandles profile =
            prefixInfo == null || !prefixInfo.isWellFormed() ?
        		  null : RawFileUtility.getRawFileHandlesFor(prefixInfo);
          return new RunDirectory(
              run, runDir, headerFile, instrumentation, source, projects, profile);
        }
      } else {
        SLLogger.getLogger().log(Level.WARNING, 
            I18N.err(107, headerFile.getAbsolutePath()));
      }
    }
    
    // If we get here there is something wrong with the profile data files
    return null;
  }
  
  private static File getFileFrom(final File runDir, final FileFilter filter,
      final int noFileErr, final int manyFilesErr) {
	  final File[] files = runDir.listFiles(filter);
	  // Must have exactly one data file
	  if (files.length == 0) { 
		  SLLogger.getLogger().log(Level.WARNING, 
				  I18N.err(noFileErr, runDir.getAbsolutePath()));
	  } else if (files.length > 1) {
		  SLLogger.getLogger().log(Level.WARNING, 
				  I18N.err(manyFilesErr, runDir.getAbsolutePath()));
	  } else { // exactly 1 (because length cannot be < 0)
		  return files[0];
	  }
	  return null;
  }
  
  /** Get the run description.  Never returns {@code null}. */
  public RunDescription getRunDescription() {
    return runDescription;
  }
  
  /** Get the handle for the run directory.  Never returns {@code null}. */
  public File getRunDirectory() {
    return runDirHandle;
  }
  
  /** Get the handle for the header file.   Never returns {@code null}. */
  public File getHeader() {
    return headerHandle;
  }
  
  /**
   * Get the handles for the instrumentation artifacts. Never returns
   * {@code null}.
   */
  public InstrumentationFileHandles getInstrumentationHandles() {
    return instrumentationFileHandles;
  }
  
  /** Get the handles for the source zip files.  Never returns {@code null}. */
  public SourceZipFileHandles getSourceHandles() {
    return sourceZipFileHandles;
  }
  
  /** Get the handles for the project jar files.  Never returns {@code null}. */
  public ProjectsDirectoryHandles getProjectDirectory() {
    return projectDirHandles;
  }
  
  /**
   * Get the handles for the profile data files. Unlike the other getter
   * methods, this method <em>may</em> return {@code null}.
   */
  public RawFileHandles getProfileHandles() {
    return rawFileHandles;
  }
}
