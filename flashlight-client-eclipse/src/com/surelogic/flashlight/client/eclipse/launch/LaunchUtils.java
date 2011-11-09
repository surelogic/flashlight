package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

  private static <T> void divideClasspath(
      final IRuntimeClasspathEntry[] entries, final Adder<T> adder,
      final List<T> user, final List<T> boot, final List<T> system) {
    for (final IRuntimeClasspathEntry entry : entries) {
      final int where = entry.getClasspathProperty();
      final File asFile = new File(entry.getLocation());
      if (asFile.exists()) {
        if (where == IRuntimeClasspathEntry.BOOTSTRAP_CLASSES) {
          adder.add(boot, entry);
        } else if (where == IRuntimeClasspathEntry.USER_CLASSES) {
          adder.add(user, entry);
        } else { // system
          adder.add(system, entry);
        }
      }
    }
  }
  
  private static abstract class Adder<T> {
    public final void add(List<T> list, IRuntimeClasspathEntry entry) {
      list.add(convertEntry(entry));
    }
    
    protected abstract T convertEntry(IRuntimeClasspathEntry entry); 
  }

  public static void divideClasspath(final IRuntimeClasspathEntry[] entries,
      final List<IRuntimeClasspathEntry> user,
      final List<IRuntimeClasspathEntry> boot,
      final List<IRuntimeClasspathEntry> system) {
    divideClasspath(entries,
        new Adder<IRuntimeClasspathEntry>() {
          @Override
          protected IRuntimeClasspathEntry convertEntry(final IRuntimeClasspathEntry entry) {
            return entry;
          }
        }, user, boot, system);
  }

  public static void divideClasspathAsLocations(final IRuntimeClasspathEntry[] entries,
      final List<String> user, final List<String> boot, final List<String> system) {
    divideClasspath(entries,
        new Adder<String>() {
          @Override
          protected String convertEntry(final IRuntimeClasspathEntry entry) {
            return entry.getLocation();
          }
        }, user, boot, system);
  }

  public static List<String> convertToLocations(
      final List<IRuntimeClasspathEntry> input) {
    final List<String> locations = new ArrayList<String>(input.size());
    for (final IRuntimeClasspathEntry entry : input) {
      locations.add(entry.getLocation());
    }
    return locations;
  }
  
  public static List<String> convertToLocations(
      final Object[] input) {
    final List<String> locations = new ArrayList<String>(input.length);
    for (final Object entry : input) {
      locations.add(((IRuntimeClasspathEntry) entry).getLocation());
    }
    return locations;
  }
}
