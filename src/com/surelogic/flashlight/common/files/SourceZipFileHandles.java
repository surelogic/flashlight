package com.surelogic.flashlight.common.files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Class for holding the file handles for the source directory as well as the
 * source zip files in that directory.
 * 
 * @see RunDirectory
 */
public final class SourceZipFileHandles {
  /** The standard name for the source zip directory. */
  public static final String SOURCE_DIR_NAME = "source";

  /** The extension we use for the source zip files */
  private static final String SRC_ZIP_EXTENSION = ".src.zip";
  
  /** The source directory file handle */
  private final File sourceDirHandle;
  private final List<File> zipHandles;
  
  
  
  /* Private constructor: use the factory method */
  private SourceZipFileHandles(final File sourceDir, final List<File> zips) {
    sourceDirHandle = sourceDir;
    zipHandles = zips;
  }

  
  
  /**
   * <i>Implementation Note:</i> This constructor scans the Flashlight data
   * directory.
   * 
   * @return The handles object.  Returns {@code null} if the source directory
   * cannot be found.
   */
  /* Package private: only to be called from RunDirectory */
  static SourceZipFileHandles getFor(final File runDir) {
	if (runDir == null || !runDir.exists()) {
		return null;
	}	  
	  
    final File sourceDir = new File(runDir, SOURCE_DIR_NAME);
    if (!sourceDir.exists()) {
      SLLogger.getLogger().log(Level.WARNING,
          I18N.err(151, runDir.getAbsolutePath(), SOURCE_DIR_NAME));
      return null;
    }
    
    /* Really the only files in the directory should be .src.zip files, but
     * we are careful just in case someone dropped some junk in the directory.
     */    
    final File[] zipFiles = sourceDir.listFiles(new FilenameFilter() {
      public boolean accept(final File dir, final String name) {
        return name.endsWith(SRC_ZIP_EXTENSION);
      }
    });
    return new SourceZipFileHandles(
        sourceDir, Collections.unmodifiableList(Arrays.asList(zipFiles)));
  }
  
  /**
   * Get the file handle for the source directory.
   */
  public File getSourceDirectory() {
    return sourceDirHandle;
  }
  
  /**
   * Get the file handles for the source zips as unmodifiable list.
   */
  public List<File> getSourceZips() {
    return zipHandles;
  }
}
