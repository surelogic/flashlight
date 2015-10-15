package com.surelogic.flashlight.client.eclipse.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.SourceZip;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDirectory;

public final class LaunchUtils {
    private LaunchUtils() {
        // do nothing;
    }

    public static IRuntimeClasspathEntry[] getClasspath(
            final ILaunchConfiguration config) {
        IRuntimeClasspathEntry[] entries;
        try {
            /*
             * This use of JavaRuntime.computeUnresolvedRuntimeClasspath() and
             * JavaRuntime.resolveRuntimeClasspath() is taken from
             * AbstractJavaLaunchConfigurationDelegate.getClasspath().
             */
            final IRuntimeClasspathEntry[] rawEntries = JavaRuntime
                    .computeUnresolvedRuntimeClasspath(config);
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

    static abstract class Adder<T> {
        public final void add(List<T> list, IRuntimeClasspathEntry entry) {
            list.add(convertEntry(entry));
        }

        protected abstract T convertEntry(IRuntimeClasspathEntry entry);
    }

    public static void divideClasspath(final IRuntimeClasspathEntry[] entries,
            final List<IRuntimeClasspathEntry> user,
            final List<IRuntimeClasspathEntry> boot,
            final List<IRuntimeClasspathEntry> system) {
        divideClasspath(entries, new Adder<IRuntimeClasspathEntry>() {
            @Override
            protected IRuntimeClasspathEntry convertEntry(
                    final IRuntimeClasspathEntry entry) {
                return entry;
            }
        }, user, boot, system);
    }

    public static void divideClasspathAsLocations(
            final IRuntimeClasspathEntry[] entries, final List<String> user,
            final List<String> boot, final List<String> system) {
        divideClasspath(entries, new Adder<String>() {
            @Override
            protected String convertEntry(final IRuntimeClasspathEntry entry) {
                return entry.getLocation();
            }
        }, user, boot, system);
    }

    public static List<String> convertToLocations(
            final List<IRuntimeClasspathEntry> input) {
        final List<String> locations = new ArrayList<>(input.size());
        for (final IRuntimeClasspathEntry entry : input) {
            locations.add(entry.getLocation());
        }
        return locations;
    }

    public static List<String> convertToLocations(final Object[] input) {
        final List<String> locations = new ArrayList<>(input.length);
        for (final Object entry : input) {
            locations.add(((IRuntimeClasspathEntry) entry).getLocation());
        }
        return locations;
    }

    /**
     * Create zips of the sources from the projects Also checks to see if it can
     * reuse an old one from the last run
     * 
     * @return Whether instrumentation was canceled.
     */
    public static boolean createSourceZips(final RunDirectory lastRun,
            final Set<IProject> projects, final File sourceDir,
            final SubMonitor progress) {
        final List<File> oldZips = lastRun == null ? Collections
                .<File> emptyList() : lastRun.getSourceZipFileHandles()
                .getSourceZips();
        for (final IProject project : projects) {
            final String projectName = project.getName();
            if (progress != null) {
                progress.subTask("Creating source zip for " + projectName);
            }
            final SourceZip srcZip = new SourceZip(project);
            final File zipFile = new File(sourceDir, projectName + ".src.zip");
            try {
                // Check if old zip is up-to-date
                File orig = null;

                for (final File old : oldZips) {
                    if (old.getName().equals(zipFile.getName())) {
                        final IJavaProject jp = JDTUtility
                                .getJavaProject(project.getName());
                        if (JDTUtility.projectUpdatedSince(jp,
                                old.lastModified())) {
                            orig = null;
                        } else {
                            orig = old;
                        }
                        break;
                    }
                }
                if (orig != null) {
                    FileUtility.copy(orig, zipFile);
                } else {
                    srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
                }
            } catch (final IOException e) {
                SLLogger.getLogger().log(
                        Level.SEVERE,
                        "Unable to create source zip for project "
                                + projectName, e);
            }
            if (progress != null) {
                if (progress.isCanceled()) {
                    return true;
                }
                progress.worked(1);
            }
        }
        return false;
    }

}
