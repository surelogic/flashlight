package com.surelogic.flashlight.common.files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Model class that holds the file handle for a project directory as well
 * as each jar file in the directory.
 * 
 * @see RunDirectory
 * @see ProjectsDirectoryHandles
 */
public final class ProjectHandles {
  /** The extension we use for the jar files */
  private static final String JAR_EXTENSION = ".jar";

  /** File handle for the project directory */
  private final File projectDirHandle;
  /** File handles for the binary directory jar files */
  private final List<File> jarHandles;
  
  
  
  /* Private: use the factory method */
  private ProjectHandles(final File projectDir, final List<File> jars) {
    projectDirHandle = projectDir;
    jarHandles = jars;
  }

  
  
  /**
   * <i>Implementation Note:</i> This constructor scans the Flashlight data
   * directory.
   * 
   * @return The handles object.  Never returns {@code null}. 
   */
  /* Package private: only to be called from ProjectsDirectoryHandles */
  static ProjectHandles getFor(final File projectDir) {
    assert projectDir != null; // should have been checked by the caller

    /* Really the only files in the directory should be .jar files, but
     * we are careful just in case someone dropped some junk in the directory.
     */    
    final File[] jarFiles = projectDir.listFiles(new FilenameFilter() {
      public boolean accept(final File dir, final String name) {
        return name.endsWith(JAR_EXTENSION);
      }
    });
    // never return null, we don't look for any specific file that must exist
    return new ProjectHandles(
        projectDir, Collections.unmodifiableList(Arrays.asList(jarFiles)));
  }
  
  
  
  /**
   * Get the file handle for the source directory.
   */
  public File getProjectDirectory() {
    return projectDirHandle;
  }
  
  /**
   * Get the file handles for the source zips as unmodifiable list.
   */
  public List<File> getJars() {
    return jarHandles;
  }
}
