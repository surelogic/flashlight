package com.surelogic.flashlight.common.files;

import static com.surelogic._flashlight.common.InstrumentationConstants.FL_STREAM_SUFFIXES;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * Model for manipulating a per-run flashlight data directory, including the
 * instrumentation artifacts, source zip files, jar files, and profile data.
 */
public final class RunDirectory {
    /** Suffix for a textual data file */
    public static final String SUFFIX = ".fl";
    /** Suffixed for a compressed textual data file */
    public static final String COMPRESSED_SUFFIX = ".fl.gz";
    /** Suffix for a binary data file */
    public static final String BIN_SUFFIX = ".flb";
    /** Suffix for a compressed binary data file */
    public static final String COMPRESSED_BIN_SUFFIX = ".flb.gz";
    /** Name of database directory under the run directory */
    private static final String DB_DIR = "db";
    private static final String QUERIES_FILE = "empty-queries.txt";
    /** Complete list of suffixes used to identify raw data files */
    private static final String[] suffixes = { COMPRESSED_SUFFIX, BIN_SUFFIX,
            SUFFIX, COMPRESSED_BIN_SUFFIX };

    public static final String HEADER_SUFFIX = ".flh";

    private static String isValidSuffix(final String name) {
        for (final String suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return suffix;
            }
        }
        return null;
    }

    /**
     * Filter used to identify files that may be raw flashlight data files.
     */
    private static final FileFilter flashlightRawDataFileFilter = new FileFilter() {
        @Override
        public boolean accept(final File pathname) {
            if (pathname.isDirectory()) {
                return false;
            }
            return isValidSuffix(pathname.getName()) != null;
        }
    };

    /**
     * Filter used to identify header files for raw flashlight data files.
     */
    private static final FileFilter flashlightHeaderFileFilter = new FileFilter() {
        @Override
        public boolean accept(final File pathname) {
            if (pathname.isDirectory()) {
                return false;
            }
            final String name = pathname.getName();
            if (name.endsWith(HEADER_SUFFIX)) {
                return true;
            }
            return false;
        }
    };

    /** The run description for the data file contained in this directory */
    private final RunDescription runDescription;

    /** The file handle for the run directory itself. */
    private final File runDirHandle;
    /** The file handle for the database */
    private final File dbHandle;
    /** The file handles for the html directory */
    private final HtmlHandles htmlHandles;
    /** The file handle for the header file */
    private final File headerHandle;
    /** The file handles for the instrumentation files. */
    private final InstrumentationFileHandles instrumentationFileHandles;
    /** The file handles for the source zips. */
    private final SourceZipFileHandles sourceZipFileHandles;
    /** The file handles for the project jar files */
    private final ProjectsDirectoryHandles projectDirHandles;
    /** The file handles for the profile data */
    private final RawFileHandles rawFileHandles;

    /* Private constructor: use the factory method */
    private RunDirectory(final RunDescription run, final File runDir,
            final File header, final File db, final HtmlHandles html,
            final InstrumentationFileHandles instrumentation,
            final SourceZipFileHandles source,
            final ProjectsDirectoryHandles projects,
            final RawFileHandles profile) {
        runDescription = run;
        headerHandle = header;
        runDirHandle = runDir;
        instrumentationFileHandles = instrumentation;
        sourceZipFileHandles = source;
        projectDirHandles = projects;
        rawFileHandles = profile;
        dbHandle = db;
        htmlHandles = html;
    }

    /**
     * Scan the given directory and gather the file handles for the contained
     * structures.
     * <p>
     * <i>Implementation Note:</i> This constructor scans the Flashlight data
     * directory.
     * 
     * @param runDir
     *            Pathname to a per-run flashlight data directory.
     * @return The model object or {@code null} if one of the necessary files
     *         doesn't exist.
     */
    /* Package private: Only created by RawFileUtility */
    static RunDirectory getFor(final File runDir) {
        // Caller checks if this null
        assert runDir != null && runDir.exists() && runDir.isDirectory();

        final InstrumentationFileHandles instrumentation = InstrumentationFileHandles
                .getFor(runDir);
        final SourceZipFileHandles source = SourceZipFileHandles.getFor(runDir);
        final ProjectsDirectoryHandles projects = ProjectsDirectoryHandles
                .getFor(runDir);

        /*
         * This process relies on RawFileUtility because RawFileHandles is the
         * original file handle class, and everything else is being built around
         * the machinery that preexisted to compute them.
         */
        final File headerFile = getFileFrom(runDir, flashlightHeaderFileFilter,
                152, 153);
        if (headerFile != null) {
            final RawDataFilePrefix headerInfo = RawFileUtility
                    .getPrefixFor(headerFile);
            if (headerInfo.isWellFormed()) {
                /*
                 * If we get here the profile data files are okay, now check
                 * that the other files are okay too.
                 */
                final File[] rawDataFiles = getFilesFrom(runDir,
                        flashlightRawDataFileFilter, 146, 147);
                if (rawDataFiles == null) {
                    return null;
                }
                final List<RawDataFilePrefix> prefixInfos = new ArrayList<RawDataFilePrefix>(
                        Arrays.asList(RawFileUtility
                                .getPrefixesFor(rawDataFiles)));
                for (Iterator<RawDataFilePrefix> iter = prefixInfos.iterator(); iter
                        .hasNext();) {
                    if (!iter.next().isWellFormed()) {
                        iter.remove();
                    }
                }
                if (prefixInfos.isEmpty()) {
                    // We don't have any valid data here
                    return null;
                }
                if (instrumentation != null && source != null
                        && projects != null) {
                    /*
                     * These calls won't fail because we know that prefixInfo is
                     * non-null and well formed.
                     */
                    final RunDescription run = RawFileUtility
                            .getRunDescriptionFor(headerInfo);
                    if (run != null) {
                        final RawFileHandles profile = RawFileUtility
                                .getRawFileHandlesFor(
                                        runDir,
                                        prefixInfos
                                                .toArray(new RawDataFilePrefix[prefixInfos
                                                        .size()]));
                        final File db = new File(runDir.getAbsoluteFile(),
                                DB_DIR);
                        final HtmlHandles html = HtmlHandles.getFor(runDir);
                        return new RunDirectory(run, runDir, headerFile, db,
                                html, instrumentation, source, projects,
                                profile);
                    }
                }
            }
        }
        // If we get here there is something wrong with the profile data files
        return null;
    }

    /**
     * Test to see whether or not a directory is not a valid run directory.
     * 
     * @param runDir
     * @return
     */
    public static boolean isInvalid(final File runDir) {
        // Caller checks if this null
        assert runDir != null && runDir.exists() && runDir.isDirectory();

        final File tag = new File(runDir,
                InstrumentationConstants.FL_COMPLETE_RUN);
        boolean valid = true;
        valid &= tag.exists();
        valid &= InstrumentationFileHandles.hasValidHandles(runDir);
        valid &= runDir.listFiles(flashlightHeaderFileFilter).length > 0;
        valid &= runDir.listFiles(flashlightRawDataFileFilter).length > 0;
        return !valid;
    }

    private static File getFileFrom(final File runDir, final FileFilter filter,
            final int noFileErr, final int manyFilesErr) {
        final File[] files = runDir.listFiles(filter);
        /*
         * files is either null, or should be a array of length 1. It should
         * only be null when we get here after a directory refresh has been
         * kicked off after a delete of a run directory.
         */
        if (files != null) {
            // Must have exactly one data file
            if (files.length == 0) {
                SLLogger.getLogger().log(Level.FINE,
                        I18N.err(noFileErr, runDir.getAbsolutePath()));
            } else if (files.length > 1) {
                SLLogger.getLogger().log(Level.FINE,
                        I18N.err(manyFilesErr, runDir.getAbsolutePath()));
            } else { // exactly 1 (because length cannot be < 0)
                return files[0];
            }
        }
        return null;
    }

    private static File[] getFilesFrom(final File runDir,
            final FileFilter filter, final int noFileErr,
            final int wrongNumFilesErr) {
        final File[] files = runDir.listFiles(filter);
        /*
         * files is either null, or should be a array of length >1. It should
         * only be null when we get here after a directory refresh has been
         * kicked off after a delete of a run directory.
         */
        if (files != null && files.length == 0) {
            SLLogger.getLogger().log(Level.FINE,
                    I18N.err(noFileErr, runDir.getAbsolutePath()));
        }
        return files;
    }

    private static boolean isValidStreamName(final String name,
            final String suffix) {
        for (final String stream : FL_STREAM_SUFFIXES) {
            if (name.endsWith(stream + suffix)) {
                return true;
            }
        }
        return false;
    }

    /** Get the run description. Never returns {@code null}. */
    public RunDescription getRunDescription() {
        return runDescription;
    }

    /** Get the handle for the run directory. Never returns {@code null}. */
    public File getRunDirectory() {
        return runDirHandle;
    }

    /**
     * Gets a human readable size of the run directory. Never returns
     * {@code null}.
     */
    public String getHumanReadableSize() {
        return FileUtility.bytesToHumanReadableString(FileUtility
                .recursiveSizeInBytes(runDirHandle));
    }

    /**
     * Gets a human readable size of the database directory. Never returns
     * {@code null}.
     */
    public String getHumanReadableDatabaseSize() {
        return FileUtility.bytesToHumanReadableString(FileUtility
                .recursiveSizeInBytes(dbHandle));
    }

    /** Get the handle for the header file. Never returns {@code null}. */
    public File getHeader() {
        return headerHandle;
    }

    /**
     * Get the handles for the instrumentation artifacts. Never returns
     * {@code null}.
     */
    public InstrumentationFileHandles getInstrumentationHandles() {
        return instrumentationFileHandles;
    }

    /** Get the handles for the source zip files. Never returns {@code null}. */
    public SourceZipFileHandles getSourceHandles() {
        return sourceZipFileHandles;
    }

    /** Get the handles for the project jar files. Never returns {@code null}. */
    public ProjectsDirectoryHandles getProjectDirectory() {
        return projectDirHandles;
    }

    /**
     * Get the handles for the profile data files. Unlike the other getter
     * methods, this method <em>may</em> return {@code null}.
     */
    public RawFileHandles getProfileHandles() {
        return rawFileHandles;
    }

    /**
     * Gets the database directory.
     * 
     * @return the non-null database directory.
     */
    public File getDatabaseDirectory() {
        return dbHandle;
    }

    /**
     * Gets the handles for the HTML directory containing an overview of the
     * findings for this run.
     * 
     * @return the non-null handles for the HTML directory.
     */
    public HtmlHandles getHtmlHandles() {
        return htmlHandles;
    }

    /**
     * The file handle for the list of queries containing no data.
     */
    public File getEmptyQueriesFile() {
        return new File(runDirHandle, QUERIES_FILE);
    }
}
