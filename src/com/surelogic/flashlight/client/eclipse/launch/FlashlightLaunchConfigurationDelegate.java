package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;
import com.surelogic.flashlight.common.FlashlightUtility;

public final class FlashlightLaunchConfigurationDelegate extends
		JavaLaunchDelegate {
	/**
	 * Returns the VM runner for the given launch mode to use when launching the
	 * given configuration.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @param mode
	 *            launch node
	 * @return VM runner to use when launching the given configuration in the
	 *         given mode
	 * @throws CoreException
	 *             if a VM runner cannot be determined
	 * @since 3.1
	 */
	@Override
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode)
			throws CoreException {
	  /* Check that the Eclipse environment meets our requirements. */
		final IVMRunner runner = checkRequirements(configuration);

		/*
     * Find the classpath entries that correspond to "binary output directories"
     * of projects in the workspace. These are the directories that we need to
     * instrument. We create two maps "userEntires" and "bootstrapEntires" that
     * we use to maintain the mapping from the original binary output directory
     * to the instrumented binary output directory we create later for entries
     * on the standard user classpath and user entries on the bootstrap
     * classpath, respectively.
     * 
     * The functionality of getClasspathEntries() and computeInstrumenationDirs()
     * can be merged.  It would be more efficient, but I think separating makes
     * the program intent cleaner.  Also, I am afraid that merging them would
     * break things.
     */
    final Map<String, String> userEntries = new HashMap<String, String>();
    final Map<String, String> userJars = new HashMap<String, String>();
    final Map<String, String> bootstrapEntries = new HashMap<String, String>();
    getClasspathEntries(configuration, userEntries, userJars, bootstrapEntries);

		/*
		 * Go through each open project and see which of the binary output
		 * directories belong to it based on the pathname prefix. Update the
		 * instrumented directory path for each binary directory.
		 */
    final String mainTypeName = getMainTypeName(configuration);
    final SimpleDateFormat dateFormat =
      new SimpleDateFormat("-yyyy.MM.dd-'at'-HH.mm.ss.SSS");
    final String datePostfix = dateFormat.format(new Date());
    final String runName = mainTypeName + datePostfix;
    final File flashlightDataDir =
      FlashlightUtility.getFlashlightDataDirectory();
    final File runOutputDir = new File(flashlightDataDir, runName);
    if (!runOutputDir.exists()) {
      runOutputDir.mkdirs();
    }
		final Set<IProject> interestingProjects = computeInstrumentationDirs(
		    configuration, runOutputDir, userEntries, userJars, bootstrapEntries);
		
		return new FlashlightVMRunner(runner, runOutputDir, userEntries, userJars,
		    bootstrapEntries,	interestingProjects, mainTypeName, datePostfix);
	}

  private IVMRunner checkRequirements(final ILaunchConfiguration configuration)
      throws CoreException {
    IVMInstall vm = verifyVMInstall(configuration);
		if (vm instanceof IVMInstall2) {
			final IVMInstall2 vm2 = (IVMInstall2) vm;
			final String javaVersion = vm2.getJavaVersion();
			int majorRel = Integer.parseInt(javaVersion.substring(2, 3));
			if (majorRel < 5) {
				throw new CoreException(SLEclipseStatusUtility
						.createErrorStatus(0,
								"Flashlight requires minimum VM version 1.5 (VM version is "
										+ javaVersion + ")."));
			}
		} else {
			throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
					"Flashlight requires minimum VM version 1.5 "
							+ "(VM version is unknown)."));
		}
		IVMRunner runner = vm.getVMRunner("run");
		if (runner == null) {
			throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
					"Failed to configure the VM to run Flashlight."));
		}
    return runner;
  }

  private void getClasspathEntries(final ILaunchConfiguration configuration,
      final Map<String, String> userEntries, final Map<String, String> userJars,
      final Map<String, String> bootstrapEntries) {
    final IRuntimeClasspathEntry[] entries = LaunchUtils.getClasspath(configuration);
    for (final IRuntimeClasspathEntry entry : entries) {
      final int where = entry.getClasspathProperty();
      final Map<String, String> entriesMap;
      final Map<String, String> jarsMap;
      if (where == IRuntimeClasspathEntry.USER_CLASSES) {
        entriesMap = userEntries;
        jarsMap = userJars;
      } else if (where == IRuntimeClasspathEntry.BOOTSTRAP_CLASSES) {
        entriesMap = bootstrapEntries;
        jarsMap = null;
      } else {
        // Standard (that is, system library) entry; ignore it
        continue;
      }
      
      final String location = entry.getLocation();
      final int type = entry.getType();
      if (location != null) {
        if (type == IRuntimeClasspathEntry.PROJECT) {
          // The project has a single binary output directory
          entriesMap.put(location, location);
        } else if (type == IRuntimeClasspathEntry.ARCHIVE) {
          /*
           * Could be a jar file in the project, or one of the
           * many binary output directories in the project. We
           * need to test if the location is a directory.
           */
          final File locationAsFile = new File(location);
          if (locationAsFile.isDirectory()) {
            entriesMap.put(location, null);
          } else {
            if (jarsMap != null) jarsMap.put(location, null);
          }
          
        }
      }
    }
  }
  
  private Set<IProject> computeInstrumentationDirs(
      final ILaunchConfiguration configuration, final File runOutputDir,
      final Map<String, String> userEntries, final Map<String, String> userJars,
      final Map<String, String> bootstrapEntries) throws CoreException {
    final File projectOutputDir = new File(runOutputDir, "projects");
    final File externalOutputDir = new File(runOutputDir, "external");
    if (!projectOutputDir.exists()) {
      projectOutputDir.mkdir();
    }

    final Set<IProject> interestingProjects = new HashSet<IProject>();
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    final IProject[] projects = root.getProjects();
    scanProjects(projects, projectOutputDir, externalOutputDir, interestingProjects, userEntries, false);
    scanProjects(projects, projectOutputDir, externalOutputDir, interestingProjects, userJars, true);
    scanProjects(projects, projectOutputDir, externalOutputDir, interestingProjects, bootstrapEntries, false);
   
    /* Filter class path items based on the user settings from the launch dialog
     * This whole process needs to be integrated better, but I'm afraid of 
     * breaking things at the moment.
     */
    List configUser = null;
    List configBootpath = null;
    try {
      configUser = configuration.getAttribute(
          PreferenceConstants.P_CLASSPATH_ENTRIES_TO_INSTRUMENT,
          Collections.emptyList());
      configBootpath = configuration.getAttribute(
          PreferenceConstants.P_BOOTPATH_ENTRIES_TO_INSTRUMENT,
          Collections.emptyList());    
    } catch (final CoreException e) {
      configUser = Collections.emptyList();
      configBootpath = Collections.emptyList();
    }

    /* This is horribly sloppy: Remove any items from the map that are not
     * in the configuration settings
     */ 
    final Iterator<Map.Entry<String, String>> userIter = userEntries.entrySet().iterator();
    while (userIter.hasNext()) {
      final Map.Entry<String, String> entry = userIter.next();
      if (!configUser.contains(entry.getKey())) userIter.remove();      
    }
    final Iterator<Map.Entry<String, String>> userJarIter = userJars.entrySet().iterator();
    while (userJarIter.hasNext()) {
      final Map.Entry<String, String> entry = userJarIter.next();
      if (!configUser.contains(entry.getKey())) userJarIter.remove();      
    }
    final Iterator<Map.Entry<String, String>> bootIter = bootstrapEntries.entrySet().iterator();
    while (bootIter.hasNext()) {
      final Map.Entry<String, String> entry = bootIter.next();
      if (!configBootpath.contains(entry.getKey())) bootIter.remove();      
    }

    return interestingProjects;
  }

  private void scanProjects(final IProject[] projects,
      final File projectOutputDir, final File externalOutputDir,
      final Set<IProject> interestingProjects,
      final Map<String, String> classpathEntries, final boolean isJars) {
    for (final IProject project : projects) {
      if (project.isOpen()) {
        final String projectLocation = project.getLocation().toOSString();
        for (final Map.Entry<String, String> entry : classpathEntries.entrySet()) {
          final String originalLocation = entry.getKey();
          if (originalLocation.startsWith(projectLocation)
              && originalLocation
                  .charAt(projectLocation.length()) == File.separatorChar) {
            // Found the project root path for the directory
            final String projectDirName = projectLocation
                .substring(projectLocation
                    .lastIndexOf(File.separatorChar) + 1);
            final String binaryName = originalLocation
                .substring(projectLocation.length() + 1);
            final String jarName = !isJars ? binaryName + ".jar" : binaryName;
            final File newLocation = new File(new File(
                projectOutputDir, projectDirName), jarName);
            entry.setValue(newLocation.getAbsolutePath());
            interestingProjects.add(project);
          }
        }
      }
    }
    
    /* Go through one more time and look for entries that weren't matched
     * with a project entry.  These correspond to classpath items that are
     * outside the workspace.  Map them to an "external" directory.
     */
    for (final Map.Entry<String, String> entry : classpathEntries.entrySet()) {
      if (entry.getValue() == null) {
    	  String entryLoc = entry.getKey();
    	  // handle Windows drive locations
    	  if (entryLoc.length() > 0) {
    	    final char driveLetter = entryLoc.charAt(0);
    	    if (driveLetter != '\\' && driveLetter != '/') {
    	      // we have a windows drive letter
    	      entryLoc = driveLetter + "-drive" + entryLoc.substring(2);
    	    }
    	  }
        final File newLocation =
          new File(externalOutputDir,
              isJars ? entryLoc : (entryLoc + ".jar"));
        entry.setValue(newLocation.getAbsolutePath());
      }
    }
  }
}