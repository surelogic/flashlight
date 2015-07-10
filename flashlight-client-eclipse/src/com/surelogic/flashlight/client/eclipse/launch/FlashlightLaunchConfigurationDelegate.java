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

import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;

public final class FlashlightLaunchConfigurationDelegate extends JavaLaunchDelegate {
  private static final int MIN_JAVA_VERSION = 6;

  /**
   * Returns the VM runner for the given launch mode to use when launching the
   * given configuration.
   *
   * @param configuration
   *          launch configuration
   * @param mode
   *          launch node
   * @return VM runner to use when launching the given configuration in the
   *         given mode
   * @throws CoreException
   *           if a VM runner cannot be determined
   * @since 3.1
   */
  @Override
  public IVMRunner getVMRunner(final ILaunchConfiguration config, final String mode) throws CoreException {
    final IVMInstall vm = verifyVMInstall(config);
    return setupFlashlightVMRunner(config, vm, getMainTypeName(config));
  }

  static IVMRunner setupFlashlightVMRunner(final ILaunchConfiguration config, final IVMInstall vm, String mainType)
      throws CoreException {
    /*
     * JUnits can be executed at the package level, and when this happens
     * mainType is not defined
     */
    if (mainType == null || "".equals(mainType)) {
      mainType = config.getName();
    }
    /* Check that the Eclipse environment meets our requirements. */
    final IVMRunner runner = checkRequirements(vm);

    /* Get the classpath. */
    final List<String> user = new ArrayList<>();
    final List<String> boot = new ArrayList<>();
    final List<String> system = new ArrayList<>();
    final IRuntimeClasspathEntry[] classpath = LaunchUtils.getClasspath(config);
    LaunchUtils.divideClasspathAsLocations(classpath, user, boot, system);

    /* Get the entries that the user does not want instrumented */
    final List<String> noInstrumentUser = config.getAttribute(FlashlightPreferencesUtility.CLASSPATH_ENTRIES_TO_NOT_INSTRUMENT,
        Collections.<String> emptyList());
    final List<String> noInstrumentBoot = config.getAttribute(FlashlightPreferencesUtility.BOOTPATH_ENTRIES_TO_NOT_INSTRUMENT,
        boot);

    /* Convert to the entries that the user does want instrumented */
    final List<String> instrumentUser = new ArrayList<>(user);
    final List<String> instrumentBoot = new ArrayList<>(boot);
    instrumentUser.removeAll(noInstrumentUser);
    instrumentBoot.removeAll(noInstrumentBoot);

    return new FlashlightVMRunner(runner, mainType, LaunchUtils.convertToLocations(classpath), instrumentUser, instrumentBoot);
  }

  static IVMRunner checkRequirements(final IVMInstall vm) throws CoreException {
    if (vm instanceof IVMInstall2) {
      final IVMInstall2 vm2 = (IVMInstall2) vm;
      final String javaVersion = vm2.getJavaVersion();
      final int majorRel = Integer.parseInt(javaVersion.substring(2, 3));
      if (majorRel < MIN_JAVA_VERSION) {
        throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
            "Flashlight requires minimum VM version 1." + MIN_JAVA_VERSION + " (VM version is " + javaVersion + ")."));
      }
    } else {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0,
          "Flashlight requires minimum VM version 1." + MIN_JAVA_VERSION + " (VM version is unknown)."));
    }
    final IVMRunner runner = vm.getVMRunner("run");
    if (runner == null) {
      throw new CoreException(SLEclipseStatusUtility.createErrorStatus(0, "Failed to configure the VM to run Flashlight."));
    }
    return runner;
  }
}