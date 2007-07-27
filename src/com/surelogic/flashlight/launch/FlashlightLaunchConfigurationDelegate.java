package com.surelogic.flashlight.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import com.surelogic.flashlight.Activator;
import com.surelogic.flashlight.FLog;
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
			IPath jarLocation = bundleBase.append("lib/flashlight.jar");
			b.append(jarLocation.toOSString());
		} else {
			throw new CoreException(
					FLog
							.createErrorStatus("No bundle location found for the Flashlight plug-in."));
		}
		final String run = getMainTypeName(configuration);
		if (run != null) {
			b.append("\" -DFL_RUN=\"");
			b.append(run);
		}
		b.append("\" -DFL_DIR=\"");
		final String rawPath = Activator.getDefault().getPluginPreferences()
				.getString(PreferenceConstants.P_RAW_PATH);
		b.append(rawPath);
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
		return b.toString();
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
				throw new CoreException(
						FLog
								.createErrorStatus("Flashlight requires minimum VM version 1.5 (VM version is "
										+ javaVersion + ")."));
			}
		} else {
			throw new CoreException(
					FLog
							.createErrorStatus("Flashlight requires minimum VM version 1.5 "
									+ "(VM version is unknown)."));
		}
		IVMRunner runner = vm.getVMRunner("run");
		if (runner == null) {
			throw new CoreException(
					FLog
							.createErrorStatus("Failed to configure the VM to run Flashlight."));
		}
		return runner;
	}
}
