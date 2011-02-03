package com.surelogic.flashlight.client.eclipse.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class FlashlightLaunchConfigurationDelegate extends
		JavaLaunchDelegate {
	private static final boolean allowJava14 = InstrumentationConstants.allowJava14;
	private static final int MIN_JAVA_VERSION = allowJava14 ? 4 : 5;

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
		return setupFlashlightVMRunner(config, vm, getMainTypeName(config));
	}

	static IVMRunner setupFlashlightVMRunner(final ILaunchConfiguration config,
			final IVMInstall vm, final String mainType) throws CoreException {
		/* Check that the Eclipse environment meets our requirements. */
		final IVMRunner runner = checkRequirements(vm);

		/* Get the classpath. */
		final List<String> user = new ArrayList<String>();
		final List<String> boot = new ArrayList<String>();
		final List<String> system = new ArrayList<String>();
		final IRuntimeClasspathEntry[] classpath = LaunchUtils
				.getClasspath(config);
		LaunchUtils.divideClasspathAsLocations(classpath, user, boot, system);

		/* Get the entries that the user does not want instrumented */
		final List noInstrumentUser = config.getAttribute(
				FlashlightPreferencesUtility.P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
				Collections.emptyList());
		final List noInstrumentBoot = config.getAttribute(
				FlashlightPreferencesUtility.P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT, boot);

		/* Convert to the entries that the user does want instrumented */
		final List<String> instrumentUser = new ArrayList<String>(user);
		final List<String> instrumentBoot = new ArrayList<String>(boot);
		instrumentUser.removeAll(noInstrumentUser);
		instrumentBoot.removeAll(noInstrumentBoot);

		final int version = getMajorJavaVersion(vm);
		return new FlashlightVMRunner(runner, mainType, LaunchUtils
				.convertToLocations(classpath), instrumentUser, instrumentBoot,
				version == 4, false);
	}

	static int getMajorJavaVersion(final IVMInstall vm) {
		if (vm instanceof IVMInstall2) {
			final IVMInstall2 vm2 = (IVMInstall2) vm;
			final String javaVersion = vm2.getJavaVersion();
			final int majorRel = Integer.parseInt(javaVersion.substring(2, 3));
			return majorRel;
		}
		return 0;
	}

	static IVMRunner checkRequirements(final IVMInstall vm)
			throws CoreException {
		if (vm instanceof IVMInstall2) {
			final IVMInstall2 vm2 = (IVMInstall2) vm;
			final String javaVersion = vm2.getJavaVersion();
			final int majorRel = Integer.parseInt(javaVersion.substring(2, 3));
			if (majorRel < MIN_JAVA_VERSION) {
				throw new CoreException(SLEclipseStatusUtility
						.createErrorStatus(0,
								"Flashlight requires minimum VM version 1."
										+ MIN_JAVA_VERSION + " (VM version is "
										+ javaVersion + ")."));
			}
		} else {
			throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
					"Flashlight requires minimum VM version 1."
							+ MIN_JAVA_VERSION + " (VM version is unknown)."));
		}
		final IVMRunner runner = vm.getVMRunner("run");
		if (runner == null) {
			throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
					"Failed to configure the VM to run Flashlight."));
		}
		return runner;
	}
}