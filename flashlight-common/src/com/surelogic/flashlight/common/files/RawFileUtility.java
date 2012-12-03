package com.surelogic.flashlight.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Utility;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * A utility designed to work with Flashlight data files and the contents of the
 * Flashlight data directory.
 */
@Utility
public final class RawFileUtility {

    /**
     * Checks if the passed raw file is compressed or not. It does this by
     * checking the file suffix (i.e., nothing fancy is done).
     * 
     * @param rawDataFile
     *            a raw data file.
     * @return {@code true} if the passed raw file is compressed, {@code false}
     *         otherwise.
     */
    public static boolean isRawFileGzip(final File rawDataFile) {
        return OutputType.detectFileType(rawDataFile).isCompressed();

    }

    /**
     * Checks to see if the passed directory appears to be a valid run directory
     * based upon its name (i.e., nothing fancy is done).
     * 
     * @param directory
     *            any directory.
     * @return {@code true} if the passed directory appears to be a valid run
     *         directory based upon its name, {@code false} otherwise.
     */
    public static boolean isRunDirectory(final File directory) {
        return f_runDirectoryFilter.accept(directory.getParentFile(),
                directory.getName());
    }

    /**
     * Gets RunDirectories for all the raw data files found in the Flashlight
     * data directory.
     * <p>
     * <i>Implementation Note:</i> This method scans the Flashlight data
     * directory.
     * 
     * @return a set of RunDirectories for all the raw data files found in the
     *         Flashlight data directory.
     */
    public static Collection<RunDirectory> getRunDirectories(final File dataDir) {
        final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader(
                dataDir);
        runDescriptionBuilder.read();
        return runDescriptionBuilder.getRunDirectories();
    }

    // TODO MOVE TO SOMEPLACE ELSE!!!
    public static RunDirectory getRunDirectoryFor(final File dataDir,
            final RunDescription description) {
        if (description == null) {
            throw new IllegalArgumentException(I18N.err(44, "description"));
        }
        final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader(
                dataDir);
        runDescriptionBuilder.read();
        return runDescriptionBuilder.getRunDirectoryFor(description);
    }

    /**
     * Reads the prefix information from a raw data file.
     * 
     * @param rawDataFile
     *            a raw data file.
     * @return prefix information (may or may not be well-formed).
     */
    public static RawDataFilePrefix getPrefixFor(final File rawDataFile) {
        if (rawDataFile == null) {
            throw new IllegalArgumentException(I18N.err(44, "dataFile"));
        }

        final RawDataFilePrefix prefixInfo = new RawDataFilePrefix();
        prefixInfo.read(rawDataFile);

        return prefixInfo;
    }

    public static RawDataFilePrefix[] getPrefixesFor(final File[] dataFiles) {
        if (dataFiles == null) {
            throw new IllegalArgumentException(I18N.err(44, "dataFiles"));
        }
        if (dataFiles.length < 1) {
            throw new IllegalArgumentException(I18N.err(92, "dataFiles"));
        }

        final RawDataFilePrefix[] rv = new RawDataFilePrefix[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            final File dataFile = dataFiles[i];
            if (dataFile == null) {
                throw new IllegalArgumentException(I18N.err(44, "dataFiles["
                        + i + "]"));
            }
            rv[i] = getPrefixFor(dataFile);
        }
        return rv;
    }

    // TODO SHOULD THIS METHOD (TAKING ONE PREFIXINFO) SHOULD BE GONE WITH NEW
    // MULTI FILE SCHEME

    /**
     * Obtains the corresponding run description for the passed raw file prefix
     * or throws an exception.
     * 
     * @param prefixInfo
     *            a well-formed raw data file prefix.
     * @return a run description based upon the passed prefix info.
     * @throws Exception
     *             if something goes wrong.
     */
    @NonNull
    public static RunDescription getRunDescriptionFor(
            final RawDataFilePrefix prefixInfo) {
        if (prefixInfo == null) {
            throw new IllegalArgumentException(I18N.err(44, "prefixInfo"));
        }

        if (prefixInfo.isWellFormed()) {
            final File runComplete = new File(prefixInfo.getFile()
                    .getParentFile(),
                    InstrumentationConstants.FL_COMPLETE_RUN_LOC);
            final boolean runCompleted = runComplete.exists();
            long duration = 0;
            if (runCompleted) {
                try {
                    BufferedReader r = new BufferedReader(new FileReader(
                            runComplete));
                    try {
                        duration = Long.parseLong(r.readLine());
                    } finally {
                        r.close();
                    }
                } catch (NumberFormatException ignore) {
                    /*
                     * We are okay with this for now, since it can happen on old
                     * runs that are otherwise valid.
                     */
                } catch (IOException e) {
                    SLLogger.getLogger().log(Level.WARNING,
                            I18N.err(226, runComplete.getAbsolutePath()), e);
                }
            }
            return new RunDescription(prefixInfo.getName(),
                    prefixInfo.getRawDataVersion(), prefixInfo.getHostname(),
                    prefixInfo.getUserName(), prefixInfo.getJavaVersion(),
                    prefixInfo.getJavaVendor(), prefixInfo.getOSName(),
                    prefixInfo.getOSArch(), prefixInfo.getOSVersion(),
                    prefixInfo.getMaxMemoryMb(), prefixInfo.getProcessors(),
                    new Timestamp(prefixInfo.getWallClockTime().getTime()),
                    duration, prefixInfo.isAndroid(), runComplete.exists());
        } else {
            throw new IllegalStateException(I18N.err(107, prefixInfo.getFile()
                    .getAbsolutePath()));
        }
    }

    /**
     * Obtains the corresponding raw file handles for the passed raw file
     * prefixes or throws an exception.
     * 
     * @param runDir
     *            a directory
     * @param prefixInfos
     *            an array of well-formed raw file prefix.
     * @return the corresponding raw file handles for the passed raw file
     *         prefix.
     * @throws Exception
     *             if something goes wrong.
     */
    @NonNull
    public static RawFileHandles getRawFileHandlesFor(final File runDir,
            final RawDataFilePrefix[] prefixInfos) {
        if (prefixInfos == null) {
            throw new IllegalArgumentException(I18N.err(44, "prefixInfos"));
        }
        if (prefixInfos.length < 1) {
            throw new IllegalArgumentException(I18N.err(92, "prefixInfos"));
        }

        for (int i = 0; i < prefixInfos.length; i++) {
            final RawDataFilePrefix p = prefixInfos[i];
            if (p == null) {
                throw new IllegalArgumentException(I18N.err(44, "prefixInfos["
                        + i + "]"));
            }
            if (!p.isWellFormed()) {
                throw new IllegalStateException(I18N.err(107, p.getFile()
                        .getAbsolutePath()));
            }
        }

        final File logFile = new File(runDir,
                InstrumentationConstants.FL_RUNTIME_LOG_LOC);
        if (!logFile.exists()) {
            SLLogger.getLogger().log(Level.FINE,
                    I18N.err(108, prefixInfos[0].getFile().getAbsolutePath()));
        }

        final RawFileHandles handles = new RawFileHandles(prefixInfos, logFile);
        return handles;
    }

    /**
     * Used to get all the run descriptions and file handles in the Flashlight
     * data directory.
     */
    private static final class RawDataDirectoryReader {

        @NonNull
        private final Map<RunDescription, RunDirectory> f_runToHandles = new HashMap<RunDescription, RunDirectory>();

        @NonNull
        private final File f_dataDir;

        RawDataDirectoryReader(final File dataDir) {
            if (dataDir == null) {
                throw new IllegalArgumentException(I18N.err(44, "dataDir"));
            }
            f_dataDir = dataDir;
        }

        @NonNull
        Collection<RunDirectory> getRunDirectories() {
            return f_runToHandles.values();
        }

        @Nullable
        RunDirectory getRunDirectoryFor(final RunDescription description) {
            return f_runToHandles.get(description);
        }

        @NonNull
        File[] getRunDirs() {
            final File[] runDirs = f_dataDir.listFiles(f_runDirectoryFilter);
            if (runDirs == null) {
                return new File[0];
            } else {
                return runDirs;
            }
        }

        void read() {
            for (final File runDir : getRunDirs()) {
                final RunDirectory runDirectory = RunDirectory.getFor(runDir);
                if (runDirectory != null) {
                    final RunDescription run = runDirectory.getDescription();
                    f_runToHandles.put(run, runDirectory);
                }
            }
        }
    }

    private static final FilenameFilter f_runDirectoryFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File root, final String name) {
            final File dir = new File(root, name);
            return dir.exists()
                    && dir.isDirectory()
                    && new File(dir, name + OutputType.FLH.getSuffix())
                            .exists();
        }
    };

    /*
     * Estimate the amount of events in the raw file based upon the size of the
     * raw file. This guess is only used for the pre-scan of the file.
     */
    public static int estimateNumEvents(final File dataFile) {
        final long sizeInBytes = dataFile.length();
        long estimatedEvents = sizeInBytes
                / (RawFileUtility.isRawFileGzip(dataFile) ? 7L : 130L);
        if (estimatedEvents <= 0) {
            estimatedEvents = 10L;
        }
        return SLUtility.safeLongToInt(estimatedEvents);
    }

    private RawFileUtility() {
        // no instances
    }
}
