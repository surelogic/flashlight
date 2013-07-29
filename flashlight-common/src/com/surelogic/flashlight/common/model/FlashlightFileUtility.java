package com.surelogic.flashlight.common.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Utility;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * A utility designed to work with Flashlight data files and the contents of the
 * Flashlight data directory.
 */
@Utility
public final class FlashlightFileUtility {

    /*
     * String constants about the prepared data directory within a Flashlight
     * run directory.
     */

    /**
     * Name of the prep directory.
     */
    private static final String PREP_DIRNAME = "prep";

    /**
     * Constructs and returns an abstract representation of the prepared data
     * directory within the passed directory.
     * <p>
     * This method does not create the directory or check if it actually exists.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return an abstract representation of the prepared data directory within
     *         the passed directory.
     */
    public static File getPrepDirectoryHandle(final File directory) {
        final File result = new File(directory, PREP_DIRNAME);
        return result;
    }

    /**
     * Name of the "data preparation is completed" file file within the prep
     * directory.
     */
    private static final String PREP_COMPLETE_FILENAME = "complete";

    /**
     * Constructs and returns an abstract representation of
     * "data preparation is completed" file within the prepared data directory
     * within the passed directory.
     * <p>
     * This method does not create the directory or check if it actually exists.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return an abstract representation of "data preparation is completed"
     *         file within the prepared data directory within the passed
     *         directory.
     */
    public static File getPrepCompleteFileHandle(final File directory) {
        final File result = new File(getPrepDirectoryHandle(directory),
                PREP_COMPLETE_FILENAME);
        return result;
    }

    /**
     * Name of the subdirectory within the prep directory that contains the
     * database.
     */
    private static final String PREP_DB_DIRNAME = "db";

    /**
     * Constructs and returns an abstract representation of the database
     * directory within the prepared data directory within the passed directory.
     * <p>
     * This method does not create the directory or check if it actually exists.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return an abstract representation of the database directory within the
     *         prepared data directory within the passed directory.
     */
    public static File getPrepDbDirectoryHandle(final File directory) {
        final File result = new File(getPrepDirectoryHandle(directory),
                PREP_DB_DIRNAME);
        return result;
    }

    /**
     * Name of the empty queries file within the prep directory.
     */
    private static final String PREP_EMPTY_QUERIES_FILENAME = "empty-queries.txt";

    /**
     * Constructs and returns an abstract representation of the empty queries
     * file within the prepared data directory within the passed directory.
     * <p>
     * This method does not create the directory or check if it actually exists.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return an abstract representation of the empty queries file within the
     *         prepared data directory within the passed directory.
     */
    public static File getPrepEmptyQueriesFileHandle(final File directory) {
        final File result = new File(getPrepDirectoryHandle(directory),
                PREP_EMPTY_QUERIES_FILENAME);
        return result;
    }

    /**
     * Name of the subdirectory within the prep directory that contains the HTML
     * for the run overview.
     */
    private static final String PREP_HTML_DIRNAME = "html";

    /**
     * Constructs and returns an abstract representation of the HTML directory
     * within the prepared data directory within the passed directory.
     * <p>
     * This method does not create the directory or check if it actually exists.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return an abstract representation of the HTML directory within the
     *         prepared data directory within the passed directory.
     */
    public static File getPrepHtmlDirectoryHandle(final File directory) {
        final File result = new File(getPrepDirectoryHandle(directory),
                PREP_HTML_DIRNAME);
        return result;
    }

    /**
     * Constructs and returns an abstract representation of the port file within
     * the passed directory.
     * <p>
     * This method does not create the directory or check if it actually exists.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return an abstract representation of the port file within the passed
     *         directory.
     */
    public static File getPortFileHandle(final File directory) {
        final File result = new File(directory,
                InstrumentationConstants.FL_PORT_FILE_LOC);
        return result;
    }

    /**
     * A heuristic used to check that we aren't still running the instrumented
     * program and collecting data into the passed directory. We do this by
     * checking if anything has been recently modified in the run directory.
     * 
     * @param directory
     *            a Flashlight run directory.
     * @return {@code true} if it appears the instrumented program is no longer
     *         collecting data into the passed directory, {@code true} if it
     *         still seems to be outputting data.
     */
    public static boolean isDoneCollectingDataInto(final File directory) {
        /*
         * Is there any data we could prep and query yet...if not we are not
         * done.
         */
        boolean hasUsableData = OutputType.COMPLETE.getFilesWithin(directory).length > 0;
        if (!hasUsableData) {
            return false;
        }
        /*
         * The port file is deleted if the collection run terminated cleanly,
         * however, if it hard crashed it stays in the directory. So if it is
         * gone, we are pretty sure the collection is completed, but if it
         * exists we can't be sure.
         */
        final boolean portFileExists = getPortFileHandle(directory).exists();
        if (hasUsableData && !portFileExists) {
            return true;
        }
        /*
         * This will detect raw files being modified, the prep subdirectory is
         * ignored.
         */
        final Set<String> ignore = new HashSet<String>();
        ignore.add(PREP_DIRNAME);
        final boolean modifiedRecently = FileUtility.anythingModifiedInTheLast(
                directory, 6, TimeUnit.SECONDS, ignore);
        // System.out.println("isDoneCollectingDataInto: modified files in last 6 s (except /prep) = "
        // + modifiedRecently);
        return !modifiedRecently;
    }

    /**
     * Finds the last ".complete" snapshot file in the passed directory and
     * returns it and its number as a pair.
     * 
     * @param directory
     *            a directory
     * @return the last ".complete" snapshot file in the passed directory and
     *         returns it and its number as a pair or {@code null} if no such
     *         file could be found.
     */
    @Nullable
    public static Pair<File, Integer> getLatestCheckpointCompleteFileAndItsNumberWithin(
            final File directory) {
        File lastCheckpoint = null;
        int lastCheckpointNum = -1;
        for (File complete : OutputType.COMPLETE.getFilesWithin(directory)) {
            final String name = complete.getName();
            if (name.length() > 15) { // ex: ...000015.complete
                String numStr = name.substring(name.length() - 15,
                        name.length() - 9);
                int thisCheckpointNum = -1;
                try {
                    thisCheckpointNum = Integer.parseInt(numStr);
                } catch (NumberFormatException ignore) {
                    SLLogger.getLogger().log(
                            Level.WARNING,
                            "Unable to parse '" + numStr
                                    + "' from checkpoint .complete file: "
                                    + name, ignore);
                }
                if (thisCheckpointNum > lastCheckpointNum) {
                    lastCheckpoint = complete;
                    lastCheckpointNum = thisCheckpointNum;
                }
            }
        }
        if (lastCheckpoint != null) {
            return new Pair<File, Integer>(lastCheckpoint, lastCheckpointNum);
        } else {
            return null;
        }
    }

    /**
     * Reads the single line within a <tt>.complete</tt> file which should look
     * something like
     * 
     * <pre>
     * 47385738473 ns
     * </pre>
     * 
     * and represents the run duration up to that snapshot in nanoseconds.
     * <p>
     * If something goes wrong a log message is generated and 0 is returned.
     * 
     * @param checkpointComplete
     *            the file to read
     * @return the duration in the passed file, or 0 if something goes wrong.
     */
    public static long readDurationInNanosFrom(final File checkpointComplete) {
        long duration = 0;
        try {
            final BufferedReader r = new BufferedReader(new FileReader(
                    checkpointComplete));
            try {
                String line = r.readLine().trim();
                final int unitsIndex = line.indexOf("ns");
                if (unitsIndex != -1) {
                    line = line.substring(0, unitsIndex).trim();
                }
                duration = Long.parseLong(line);
            } finally {
                r.close();
            }
        } catch (Exception e) {
            SLLogger.getLogger().log(Level.WARNING,
                    I18N.err(226, checkpointComplete.getAbsolutePath()), e);
        }
        return duration;
    }

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

    /**
     * Reads the prefix information from a raw data file.
     * 
     * @param rawDataFile
     *            a raw data file.
     * @return prefix information (may or may not be well-formed).
     */
    public static CheckpointFilePrefix getPrefixFor(final File rawDataFile) {
        if (rawDataFile == null) {
            throw new IllegalArgumentException(I18N.err(44, "dataFile"));
        }

        final CheckpointFilePrefix prefix = new CheckpointFilePrefix();
        prefix.read(rawDataFile);

        return prefix;
    }

    /**
     * Obtains the corresponding raw file handles for the passed raw file
     * prefixes or throws an exception.
     * 
     * @param directory
     *            a run directory.
     * @param lastCheckpointCompleteFileNumber
     *            the number from the last ".complete" file in the directory,
     *            obtained by calling
     *            {@link #getLatestCheckpointCompleteFileAndItsNumberWithin(File)}
     *            .
     * @return a raw file handles.
     * 
     * @throws Exception
     *             if something goes wrong.
     */
    @NonNull
    public static RawFileHandles getRawFileHandlesFor(final File directory,
            final int lastCheckpointCompleteFileNumber) {
        /**
         * Filter used to identify files that may be raw flashlight data files.
         */
        final FileFilter flashlightRawDataFileFilter = new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                if (pathname.isDirectory()) {
                    return false;
                }
                return OutputType.mayBeRawDataFile(pathname);
            }
        };
        final ArrayList<File> rawFiles = new ArrayList<File>();
        for (File raw : directory.listFiles(flashlightRawDataFileFilter)) {
            rawFiles.add(raw);
        }

        final File[] orderedRawFiles = new File[lastCheckpointCompleteFileNumber + 1];
        for (int i = 0; i <= lastCheckpointCompleteFileNumber; i++) {
            File rawThisNum = null;
            for (Iterator<File> iterator = rawFiles.iterator(); iterator
                    .hasNext();) {
                final File raw = iterator.next();
                final int fileNum = getDataFileNumHelper(raw);
                if (fileNum == i) {
                    rawThisNum = raw;
                    iterator.remove();
                }
            }
            if (rawThisNum != null) {
                orderedRawFiles[i] = rawThisNum;
            } else {
                throw new IllegalStateException(I18N.err(108, i,
                        directory.getAbsolutePath()));
            }
        }

        final RawFileHandles handles = new RawFileHandles(orderedRawFiles,
                OutputType.LOG.getFilesWithin(directory));
        return handles;
    }

    private static int getDataFileNumHelper(@NonNull final File file) {
        int result = -1;
        if (file == null) {
            return result;
        }

        final String name = file.getName();
        if (name == null) {
            return result;
        }

        final OutputType type = OutputType.detectFileType(file);
        if (type == null || !OutputType.RAW_DATA.contains(type)) {
            return result;
        }

        // remove extension
        int endIndex = name.length() - type.getSuffix().length();
        int beginIndex = endIndex - 6;
        final String numStr = name.substring(beginIndex, endIndex);

        try {
            result = Integer.parseInt(numStr);
        } catch (NumberFormatException ignore) {
            SLLogger.getLogger().log(
                    Level.WARNING,
                    "Unable to parse '" + numStr + "' from raw data file: "
                            + name, ignore);
        }

        return result;
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
                final RunDirectory runDirectory = RunDirectory.getFor(runDir,
                        true);
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
            final File potentialRunDir = new File(root, name);
            final boolean result = potentialRunDir.isDirectory()
                    && isDoneCollectingDataInto(potentialRunDir);
            // System.out.println("Considered " + potentialRunDir.getName() +
            // " : accept=" + result);
            return result;
        }
    };

    /**
     * Estimate the amount of events in the raw file based upon the size of the
     * raw file. This guess is only used for the pre-scan of the file during the
     * prep.
     * 
     * @param dataFile
     *            a raw file.
     * @return a guess of how many events are in the file.
     */
    public static int estimateNumEvents(final File dataFile) {
        final long sizeInBytes = dataFile.length();
        long estimatedEvents = sizeInBytes
                / (FlashlightFileUtility.isRawFileGzip(dataFile) ? 7L : 130L);
        if (estimatedEvents <= 0) {
            estimatedEvents = 10L;
        }
        return SLUtility.safeLongToInt(estimatedEvents);
    }

    public static int estimateNumEvents(final Collection<File> dataFiles) {
        long allEstimatedEvents = 0;
        for (File dataFile : dataFiles) {
            final long sizeInBytes = dataFile.length();
            long estimatedEvents = sizeInBytes
                    / (FlashlightFileUtility.isRawFileGzip(dataFile) ? 7L
                            : 130L);
            if (estimatedEvents <= 0) {
                estimatedEvents = 10L;
            }
            allEstimatedEvents += estimatedEvents;
        }
        return SLUtility.safeLongToInt(allEstimatedEvents);
    }

    /*
     * Helper code for run identity strings.
     */

    /**
     * Gets if the passed run identity string is from an instrumented Android
     * application. If not, it is is from an instrumented standard Java program.
     * 
     * @param runIdString
     *            a run identity string.
     * @return {@code true} if this run is from an instrumented Android
     *         application, {@code false} if this run is from an instrumented
     *         standard Java program.
     */
    public static boolean isAndroid(@NonNull final String runIdString) {
        if (runIdString == null) {
            throw new IllegalArgumentException(I18N.err(44, "runIdString"));
        }
        return runIdString
                .endsWith(InstrumentationConstants.ANDROID_LAUNCH_SUFFIX);
    }

    /**
     * Gets the name of the run from the passed run identity string by removing
     * the date at the end. This is typically the fully-qualified class name for
     * a Java application or the project name for an Android application.
     * 
     * @param runIdString
     *            a run identity string.
     * @return the name of the run from the passed run identity string.
     */
    @NonNull
    public static String getRunName(@NonNull final String runIdString) {
        if (runIdString == null) {
            throw new IllegalArgumentException(I18N.err(44, "runIdString"));
        }
        int backFromEnd = InstrumentationConstants.DATE_FORMAT.length() - 2;
        backFromEnd += isAndroid(runIdString) ? InstrumentationConstants.ANDROID_LAUNCH_SUFFIX
                .length() : InstrumentationConstants.JAVA_LAUNCH_SUFFIX
                .length();
        final int index = runIdString.length() - backFromEnd;
        if (index < 0) {
            throw new IllegalArgumentException(I18N.err(248, runIdString,
                    backFromEnd));
        }
        return runIdString.substring(0, index);
    }

    /**
     * Gets a simple run name for the passed run name. For example
     * <tt>com.surelogic.Program</tt> would return <tt>Program</tt>. This
     * shortens names of Java applications which use the fully-qualified class
     * name as a name.
     * 
     * @param runName
     *            a run name.
     * @return a simple run name for the passed run name
     * 
     * @see #getRunName(String)
     */
    @NonNull
    public static String getSimpleRunName(@NonNull final String runName) {
        final int dotIndex = runName.lastIndexOf('.');
        if (dotIndex == -1 || runName.length() <= dotIndex) {
            return runName;
        } else {
            return runName.substring(dotIndex + 1);
        }
    }

    private FlashlightFileUtility() {
        // no instances
    }
}
