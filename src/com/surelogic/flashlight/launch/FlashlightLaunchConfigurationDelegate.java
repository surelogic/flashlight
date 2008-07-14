package com.surelogic.flashlight.launch;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import com.surelogic.common.eclipse.jdt.SourceZip;
import com.surelogic.common.eclipse.logging.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.Activator;
import com.surelogic.flashlight.preferences.PreferenceConstants;

public final class FlashlightLaunchConfigurationDelegate extends
		JavaLaunchDelegate {

	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		StringBuilder b = new StringBuilder(super.getVMArguments(configuration));
		b.append(" -javaagent:\"");
		IPath bundleBase = Activator.getDefault().getBundleLocation();
		if (bundleBase != null) {
			IPath jarLocation = bundleBase.append("lib/flashlight-asm-all.jar");
			b.append(jarLocation.toOSString());
		} else {
			throw new CoreException(SLStatus.createErrorStatus(0,
					"No bundle location found for the Flashlight plug-in."));
		}
		final String run = getMainTypeName(configuration);
		if (run != null) {
			b.append("\" -DFL_RUN=\"");
			b.append(run);
		}
		b.append("\" -DFL_DIR=\"");
		b.append(PreferenceConstants.getFlashlightRawDataPath());
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
				throw new CoreException(SLStatus.createErrorStatus(0,
						"Flashlight requires minimum VM version 1.5 (VM version is "
								+ javaVersion + ")."));
			}
		} else {
			throw new CoreException(SLStatus.createErrorStatus(0,
					"Flashlight requires minimum VM version 1.5 "
							+ "(VM version is unknown)."));
		}
		IVMRunner runner = vm.getVMRunner("run");
		if (runner == null) {
			throw new CoreException(SLStatus.createErrorStatus(0,
					"Failed to configure the VM to run Flashlight."));
		}

		// Create source zip
		final StringBuilder fileName = new StringBuilder();
		final String rawPath = PreferenceConstants.getFlashlightRawDataPath();
		if (rawPath != null) {
			fileName.append(rawPath);
		} else {
			fileName.append(System.getProperty("java.io.tmpdir"));
		}
		fileName.append(System.getProperty("file.separator"));
		fileName.append(getMainTypeName(configuration));

		final SimpleDateFormat dateFormat = new SimpleDateFormat(
				"-yyyy.MM.dd-'at'-HH.mm.ss.SSS");
		// make the filename and time event times match
		Date now = new Date();
		fileName.append(dateFormat.format(now));
		fileName.append(".src.zip");

		final String projAttr = "org.eclipse.jdt.launching.PROJECT_ATTR";
		final String projectName = configuration.getAttribute(projAttr, "");
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		// FIX this is not right for projects with dependencies
		if (project != null && project.exists()) {
			try {
				SourceZip
						.generateSourceZip(fileName.toString(), project, false);
			} catch (IOException e) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Unable to create source zip", e);
			}
		} else {
			SLLogger.getLogger().log(Level.SEVERE,
					"No such project: " + projectName);
		}
		return runner;
	}
}
