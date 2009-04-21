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

import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;
import com.surelogic.flashlight.client.eclipse.preferences.PreferenceConstants;

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
	public IVMRunner getVMRunner(
	    final ILaunchConfiguration configuration, final String mode)
	throws CoreException {
	  /* Check that the Eclipse environment meets our requirements. */
		final IVMRunner runner = checkRequirements(configuration);

		
		/* Get the classpath. */
    final List<String> user = new ArrayList<String>();
    final List<String> boot = new ArrayList<String>();
    final List<String> system = new ArrayList<String>();
		final IRuntimeClasspathEntry[] classpath = LaunchUtils.getClasspath(configuration);
		LaunchUtils.divideClasspathAsLocations(classpath, user, boot, system);
    
    /* Get the entries that the user does not want instrumented */
    final List noInstrumentUser = configuration.getAttribute(
          PreferenceConstants.P_CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
          user);
    final List noInstrumentBoot = configuration.getAttribute(
          PreferenceConstants.P_BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
          Collections.emptyList());
    
    /* Convert to the entries that the user does want instrumented */
    final List<String> instrumentUser = new ArrayList<String>(user);
    final List<String> instrumentBoot = new ArrayList<String>(boot);
    instrumentUser.removeAll(noInstrumentUser);
    instrumentBoot.removeAll(noInstrumentBoot);
    
    return new FlashlightVMRunner(runner, getMainTypeName(configuration),
        user, boot, system, instrumentUser, instrumentBoot);
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
}