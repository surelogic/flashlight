package com.surelogic.flashlight.common.files;

import java.io.File;
import java.io.FileFilter;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDescription;

public final class RawFileUtility {

	public static final String SUFFIX = ".fl";
	public static final String COMPRESSED_SUFFIX = ".fl.gz";

	/**
	 * Gets descriptions for all the raw data files found in the Flashlight data
	 * directory.
	 * 
	 * @return a set of descriptions for all the raw data files found in the
	 *         Flashlight data directory.
	 */
	public static Set<RunDescription> getRunDescriptions() {
		final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader();
		runDescriptionBuilder.read();
		return runDescriptionBuilder.getRunDescriptions();
	}

	/**
	 * Examines the Flashlight data directory and returns file handles
	 * corresponding to the passed run description, or {@code null} if no file
	 * handles exist.
	 * 
	 * @param description
	 *            a non-null run description.
	 * @return an object containing file handles to the raw data file and its
	 *         associated log file, or {@code null} if no file handles exist.
	 */
	public static RawFileHandles getRawFileHandlesFor(
			final RunDescription description) {
		if (description == null)
			throw new IllegalArgumentException(I18N.err(44, "description"));
		final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader();
		runDescriptionBuilder.read();
		return runDescriptionBuilder.getRawFileHandlesFor(description);
	}

	private static class RawDataDirectoryReader {

		final Set<RunDescription> f_runs = new HashSet<RunDescription>();

		Set<RunDescription> getRunDescriptions() {
			return f_runs;
		}

		private final Map<RunDescription, RawFileHandles> f_runToHandles = new HashMap<RunDescription, RawFileHandles>();

		RawFileHandles getRawFileHandlesFor(final RunDescription description) {
			return f_runToHandles.get(description);
		}

		void read() {
			final File[] dataFiles = RawFileUtility.getRawDataFiles();
			for (final File f : dataFiles) {
				add(f);
			}
		}

		private void add(final File dataFile) {
			assert dataFile != null;

			final RawDataFilePrefix prefixInfo = new RawDataFilePrefix();
			prefixInfo.read(dataFile);

			/*
			 * If we got all the data we needed from the file go ahead and add
			 * this raw file information to what we are collecting.
			 */
			if (prefixInfo.isWellFormed()) {
				String fileNamePrefix = dataFile.getAbsolutePath();
				if (dataFile.getName().endsWith(".fl")) {
					fileNamePrefix = fileNamePrefix.substring(0, fileNamePrefix
							.length() - 3);
				} else {
					fileNamePrefix = fileNamePrefix.substring(0, fileNamePrefix
							.length() - 6);
				}
				final File logFile = new File(fileNamePrefix + ".flog");
				if (!logFile.exists()) {
					SLLogger.getLogger().log(Level.WARNING,
							I18N.err(108, dataFile.getAbsolutePath()));
				}

				final Timestamp started = new Timestamp(prefixInfo
						.getWallClockTime().getTime());
				final RunDescription run = new RunDescription(prefixInfo
						.getName(), prefixInfo.getRawDataVersion(), prefixInfo
						.getUserName(), prefixInfo.getJavaVersion(), prefixInfo
						.getJavaVendor(), prefixInfo.getOSName(), prefixInfo
						.getOSArch(), prefixInfo.getOSVersion(), prefixInfo
						.getMaxMemoryMb(), prefixInfo.getProcessors(), started);
				f_runs.add(run);
				final RawFileHandles handles = new RawFileHandles(dataFile,
						logFile);
				f_runToHandles.put(run, handles);
			} else {
				SLLogger.getLogger().log(Level.WARNING,
						I18N.err(107, dataFile.getAbsolutePath()));
			}
		}
	}

	/**
	 * @see #getRawDataFiles()
	 */
	private static final FileFilter f_flashlightRawDataFileFilter = new FileFilter() {
		public boolean accept(File pathname) {
			final String name = pathname.getName();
			return name.endsWith(COMPRESSED_SUFFIX) || name.endsWith(SUFFIX);
		}
	};

	/**
	 * @return the set of Flashlight raw data files (compressed or uncompressed)
	 *         found in the Flashlight directory.
	 */
	private static File[] getRawDataFiles() {
		final File directory = new File(FileUtility
				.getFlashlightDataDirectory());
		final File[] dataFiles = directory
				.listFiles(f_flashlightRawDataFileFilter);
		return dataFiles;
	}

	private RawFileUtility() {
		// no instances
	}
}
