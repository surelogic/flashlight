package com.surelogic.flashlight.client.eclipse.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;

public class FlashlightMonitorJUnitLaunchConfigurationDelegate extends
		JUnitLaunchConfigurationDelegate {
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
	public IVMRunner getVMRunner(final ILaunchConfiguration config,
			final String mode) throws CoreException {
		final IVMInstall vm = verifyVMInstall(config);
		return FlashlightMonitorLaunchConfigurationDelegate
				.setupFlashlightVMRunner(config, vm, getMainTypeName(config));
	}
}
