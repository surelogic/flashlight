package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

import com.surelogic._flashlight.rewriter.Configuration;
import com.surelogic._flashlight.rewriter.EngineMessenger;
import com.surelogic._flashlight.rewriter.PrintWriterMessenger;
import com.surelogic._flashlight.rewriter.RewriteEngine;
import com.surelogic.common.FileUtility;
import com.surelogic.common.eclipse.jdt.SourceZip;
import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.Activator;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

public final class FlashlightLaunchConfigurationDelegate extends
		JavaLaunchDelegate {

	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		StringBuilder b = new StringBuilder(super.getVMArguments(configuration));
//		b.append(" -javaagent:\"");
//		IPath bundleBase = Activator.getDefault().getBundleLocation();
//		if (bundleBase != null) {
//			IPath jarLocation = bundleBase.append("lib/flashlight-all.jar");
//			b.append(jarLocation.toOSString());
//		} else {
//			throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
//					"No bundle location found for the Flashlight plug-in."));
//		}
		final String run = getMainTypeName(configuration);
		if (run != null) {
			b.append("\" -DFL_RUN=\"");
			b.append(run);
		}
		b.append("\" -DFL_DIR=\"");
		b.append(FileUtility.getFlashlightDataDirectory());
		b.append("\" -DFL_RAWQ_SIZE=");
		final String rawQSize = Activator.getDefault().getPluginPreferences()
				.getString(PreferenceConstants.P_RAWQ_SIZE);
		b.append(rawQSize);
		b.append(" -DFL_REFINERY_SIZE=");
		final String refSize = Activator.getDefault().getPluginPreferences()
				.getString(PreferenceConstants.P_REFINERY_SIZE);
		b.append(refSize);
		b.append(" -DFL_OUTQ_SIZE=");
		final String outQSize = Activator.getDefault().getPluginPreferences()
				.getString(PreferenceConstants.P_OUTQ_SIZE);
		b.append(outQSize);
		b.append(" -DFL_CONSOLE_PORT=");
		final String cPort = Activator.getDefault().getPluginPreferences()
				.getString(PreferenceConstants.P_CONSOLE_PORT);
		b.append(cPort);
		final boolean useSpy = Activator.getDefault().getPluginPreferences()
				.getBoolean(PreferenceConstants.P_USE_SPY);
		if (!useSpy) {
			b.append(" -DFL_NO_SPY=true");
		}
		String result = b.toString();
		// System.out.println("Flashlight VM args: "+result);
		return result;
	}

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
		
		
		
		// Instrument classfiles
    IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
    entries = JavaRuntime.resolveRuntimeClasspath(entries, configuration);
    final Map<String, String> projectEntries = new HashMap<String, String>();
    for (final IRuntimeClasspathEntry entry : entries) {
      if (entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES &&
          entry.getType() == IRuntimeClasspathEntry.PROJECT) {
        final String location = entry.getLocation();
        if (location != null) {
          // Based on the implementation in AbstractJavaLauchConfiguration.getClassPath(), it may be possible to have duplicate directory locations.  We eat the duplicates.
          projectEntries.put(location, location);
        }
      }
    }
    
    /* We go through some hoops to find the project names.  It's hard to get
     * these directly from the locations above, because we do not know how many
     * levels deep an binary output directory may be nested.
     */
    final String runName = mainTypeName + datePostfix;
    final File runOutputDir = new File(tmpDirName, runName);
    final File projectOutputDir = new File(runOutputDir, "projects");
    
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
            break;
          }
        }
      }
    }
      
    return new FlashlightVMRunner(runner, runOutputDir, projectEntries);
//		return runner;
	}
}