package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import com.surelogic.common.logging.SLLogger;

public final class LaunchUtils {
  private LaunchUtils() {
    // do nothing;
  }
  
  
  public static IRuntimeClasspathEntry[] getClasspath(
      final ILaunchConfiguration config) {
    IRuntimeClasspathEntry[] entries;
    try {
      /* This use of JavaRuntime.computeUnresolvedRuntimeClasspath() and
       * JavaRuntime.resolveRuntimeClasspath() is taken from
       * AbstractJavaLaunchConfigurationDelegate.getClasspath().
       */
      final IRuntimeClasspathEntry[] rawEntries =
        JavaRuntime.computeUnresolvedRuntimeClasspath(config);
      entries = JavaRuntime.resolveRuntimeClasspath(rawEntries, config);
    } catch (final CoreException e) {
      // Cleanup state
      entries = new IRuntimeClasspathEntry[0];
      
      // Report error
      SLLogger.getLogger().log(Level.WARNING,
          "Error while getting classpath", e);
    }
    return entries;
  }

  public static void divideClasspath(final IRuntimeClasspathEntry[] entries,
      final java.util.List<IRuntimeClasspathEntry> user,
      final java.util.List<IRuntimeClasspathEntry> boot) {
    for (final IRuntimeClasspathEntry entry : entries) {
      final int where = entry.getClasspathProperty();
      final File asFile = new File(entry.getLocation());
      if (asFile.exists()) {
        if (where == IRuntimeClasspathEntry.BOOTSTRAP_CLASSES) {
          boot.add(entry);
        } else if (where == IRuntimeClasspathEntry.USER_CLASSES) {
          user.add(entry);
        }
      }
    }
  }
  
  public static java.util.List<String> convertToLocations(
      final java.util.List<IRuntimeClasspathEntry> input) {
    final java.util.List<String> locations = new ArrayList<String>(input.size());
    for (final IRuntimeClasspathEntry entry : input) {
      locations.add(entry.getLocation());
    }
    return locations;
  }
  
  public static java.util.List<String> convertToLocations(
      final Object[] input) {
    final java.util.List<String> locations = new ArrayList<String>(input.length);
    for (final Object entry : input) {
      locations.add(((IRuntimeClasspathEntry) entry).getLocation());
    }
    return locations;
  }
}
