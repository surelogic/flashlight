package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.jdt.SourceZip;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;

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

		
		
		// Really need a progress monitor for all this stuff
		final String tmpDirName = System.getProperty("java.io.tmpdir");
    final SimpleDateFormat dateFormat = new SimpleDateFormat("-yyyy.MM.dd-'at'-HH.mm.ss.SSS");
		final String datePostfix = dateFormat.format(new Date());
		final String mainTypeName = getMainTypeName(configuration);
		
		
		/* This is old and should be removed, but VisualVM support needs it? */
		
		// Create source zip
		final StringBuilder fileName = new StringBuilder();
		final String rawPath = FileUtility.getFlashlightDataDirectory();
		if (rawPath != null) {
			fileName.append(rawPath);
		} else {
			fileName.append(tmpDirName);
		}
		fileName.append(File.separatorChar);
		fileName.append(mainTypeName);
		fileName.append(datePostfix);
		fileName.append(".src.zip");
		
		final String projAttr = "org.eclipse.jdt.launching.PROJECT_ATTR";
		final String projectName = configuration.getAttribute(projAttr, "");
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject runningFromProject = root.getProject(projectName);
		// FIX this is not right for projects with dependencies
		if (runningFromProject != null && runningFromProject.exists()) {
			try {
				new SourceZip(runningFromProject).generateSourceZip(fileName.toString(), runningFromProject, false);
			} catch (IOException e) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Unable to create source zip", e);
			}
		} else {
			SLLogger.getLogger().log(Level.SEVERE,
					"No such project: " + projectName);
		}

    /* END OLD */

		
		/* Find the classpath entries that correspond to "binary output directories"
		 * of projects in the workspace.  These are the directories that we need
		 * to instrument.  This use of JavaRuntime.computeUnresolvedRuntimeClasspath()
		 * and JavaRuntime.resolveRuntimeClasspath() is taken from 
		 * AbstractJavaLaunchConfigurationDelegate.getClasspath().  We create a
		 * map "projectEntries" that we will use to maintain the mapping from the
		 * original binary output directory to the instrumented binary output
		 * directory we create later.
		 */

    IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
    entries = JavaRuntime.resolveRuntimeClasspath(entries, configuration);
    final Map<String, String> projectEntries = new HashMap<String, String>();
    for (final IRuntimeClasspathEntry entry : entries) {
      if (entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
        final String location = entry.getLocation();
        if (location != null) {
          if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
            // The project has a single binary output directory
            projectEntries.put(location, location);
          } else if (entry.getType() == IRuntimeClasspathEntry.ARCHIVE) {
            /* Could be a jar file in the project, or one of the many 
             * binary output directories in the project.  We need to test if
             * the location is a directory.
             */
            final File locationAsFile = new File(location);
            if (locationAsFile.isDirectory()) {
              projectEntries.put(location, location);
            }
          }
        }
      }      
    }
    
    /* Go through each open project and see which of the binary output
     * directories belong to it based on the pathname prefix.  Update the
     * instrumented directory path for each binary directory.  
     */
    final String runName = mainTypeName + datePostfix;
    final File runOutputDir = new File(tmpDirName, runName);
    final File projectOutputDir = new File(runOutputDir, "projects");
    final Set<IProject> interestingProjects = new HashSet<IProject>();
    final IProject[] projects = root.getProjects();
    for (final IProject project : projects) {
      if (project.isOpen()) {
        final String projectLocation = project.getLocation().toOSString();
        for (final Map.Entry<String, String> entry : projectEntries.entrySet()) {
          final String originalLocation = entry.getKey();
          if (originalLocation.startsWith(projectLocation) && originalLocation.charAt(projectLocation.length()) == File.separatorChar) {
            // Found the project root path for the directory
            final String projectDirName = projectLocation.substring(projectLocation.lastIndexOf(File.separatorChar) + 1);
            final String binaryDirName = originalLocation.substring(projectLocation.length() + 1);
            final File newLocation = new File(new File(projectOutputDir, projectDirName), binaryDirName);
            entry.setValue(newLocation.getAbsolutePath());
            interestingProjects.add(project);
          }
        }
      }
    }
      
    return new FlashlightVMRunner(runner, runOutputDir, projectEntries, interestingProjects, mainTypeName);
//		return runner;
	}
}