package com.surelogic.flashlight.common.files;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Class for holding the file handle for the projects directory as well as
 * each subsidiary project directory.
 * 
 * @see RunDirectory
 */
public final class ProjectsDirectoryHandles {
  /** The standard name for the projects directory. */
  public static final String PROJECTS_DIR_NAME = "projects";

  /** The file handle to the projects directory */
  private final File projectsDirHandle;
  /** The file handles for the individual project directories */
  private final List<ProjectHandles> projectHandles;
  
  
  
  /* Private constructor: use the factory method */
  private ProjectsDirectoryHandles(
      final File projectsDir, final List<ProjectHandles> projects) {
    projectsDirHandle = projectsDir;
    projectHandles = projects;
  }

  
  
  /**
   * <i>Implementation Note:</i> This constructor scans the Flashlight data
   * directory.
   * 
   * @return The handles object.  Returns {@code null} if the projects directory
   * cannot be found.
   */
  /* Package private: only to be called from RunDirectory */
  static ProjectsDirectoryHandles getFor(final File runDir) {
    final File projectsDir = new File(runDir, PROJECTS_DIR_NAME);
    if (runDir == null || !runDir.exists()) {
    	return null;
    }
    if (!projectsDir.exists()) {
      SLLogger.getLogger().log(Level.WARNING,
          I18N.err(151, runDir.getAbsolutePath(), PROJECTS_DIR_NAME));
      return null;
    }
    
    /* Really the only files in the directory should be per-project directories,
     * but we are careful to just gather directories in case someone dropped some
     * junk in the folder.
     */    
    final File[] projectDirs = projectsDir.listFiles(new FileFilter() {
      public boolean accept(final File pathname) {
        return pathname.isDirectory();
      }
    });
    final ArrayList<ProjectHandles> projectHandlesTemp =
      new ArrayList<ProjectHandles>(projectDirs.length);
    for (final File projectDir : projectDirs) {
      // ProjectHandles.getFor cannot fail
      projectHandlesTemp.add(ProjectHandles.getFor(projectDir));
    }
    return new ProjectsDirectoryHandles(
        projectsDir, Collections.unmodifiableList(projectHandlesTemp));
  }

  
  
  public File getProjectsDirectory() {
    return projectsDirHandle;
  }
  
  public List<ProjectHandles> getProjects() {
    return projectHandles;
  }
}
