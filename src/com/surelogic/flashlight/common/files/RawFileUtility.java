package com.surelogic.flashlight.common.files;

import java.io.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.BinaryEventReader;
import com.surelogic.common.FileUtility;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * A utility designed to work with Flashlight data files and the contents of the
 * Flashlight data directory.
 */
public final class RawFileUtility {
  public static final String DB_DIRECTORY = "db";
  
	public static final String SUFFIX = ".fl";
	public static final String COMPRESSED_SUFFIX = ".fl.gz";
	
	public static final String BIN_SUFFIX = ".flb";
	public static final String COMPRESSED_BIN_SUFFIX = ".flb.gz";
	
	private static final String[] suffixes = {
		COMPRESSED_SUFFIX, BIN_SUFFIX, SUFFIX, COMPRESSED_BIN_SUFFIX
	};

	/**
	 * Checks if the passed raw file is compressed or not. It does this by
	 * checking the file suffix (i.e., nothing fancy is done).
	 * 
	 * @param dataFile
	 *            a raw data file.
	 * @return {@code true} if the passed raw file is compressed, {@code false}
	 *         otherwise.
	 */
	public static boolean isRawFileGzip(final File dataFile) {
		final String name = dataFile.getName();
		return name.endsWith(COMPRESSED_SUFFIX) || name.endsWith(COMPRESSED_BIN_SUFFIX);
	}

	static boolean isBinary(final File dataFile) {
		final String name = dataFile.getName();
		return name.endsWith(BIN_SUFFIX) || name.endsWith(COMPRESSED_BIN_SUFFIX);
	}
	
	/**
	 * Gets an input stream to read the passed raw file. This method opens the
	 * right kind of stream based upon if the raw file is compressed or not.
	 * 
	 * @param dataFile
	 *            a raw data file.
	 * @return an input stream to read the passed raw file.
	 * @throws IOException
	 *             if the file doesn't exist or some other IO problem occurs.
	 */
	public static InputStream getInputStreamFor(final File dataFile)
			throws IOException {
		InputStream stream = new FileInputStream(dataFile);
		if (isRawFileGzip(dataFile)) {
			stream = new GZIPInputStream(stream, 32 * 1024);
		} else {
			stream = new BufferedInputStream(stream, 32 * 1024);
		}
		if (isBinary(dataFile)) {
			stream = new ObjectInputStream(stream);
		}
		return stream;
	}

	/**
	 * Gets descriptions for all the raw data files found in the Flashlight data
	 * directory.
	 * <p>
	 * <i>Implementation Note:</i> This method scans the Flashlight data
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

//	/**
//	 * Examines the Flashlight data directory and returns file handles
//	 * corresponding to the passed run description, or {@code null} if no file
//	 * handles exist.
//	 * <p>
//	 * <i>Implementation Note:</i> This method scans the Flashlight data
//	 * directory.
//	 * 
//	 * @param description
//	 *            a non-null run description.
//	 * @return an object containing file handles to the raw data file and its
//	 *         associated log file, or {@code null} if no file handles exist.
//	 */
//	public static RawFileHandles getRawFileHandlesFor(
//			final RunDescription description) {
//		if (description == null)
//			throw new IllegalArgumentException(I18N.err(44, "description"));
//		final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader();
//		runDescriptionBuilder.read();
//		return runDescriptionBuilder.getRawFileHandlesFor(description);
//	}

	public static RunDirectory getRunDirectoryFor(
	    final RunDescription description) {
    if (description == null)
      throw new IllegalArgumentException(I18N.err(44, "description"));
    final RawDataDirectoryReader runDescriptionBuilder = new RawDataDirectoryReader();
    runDescriptionBuilder.read();
    return runDescriptionBuilder.getRunDirectoryFor(description);
  }

	public static RunDirectory getRunDirectoryFor(
	    final RawDataFilePrefix prefixInfo) {
	  return getRunDirectoryFor(getRunDescriptionFor(prefixInfo));
	}
	
	/**
   * Reads the prefix information from a raw data file.
   * 
   * @param dataFile
   *          a raw data file.
   * @return prefix information (may or may not be well-formed).
   */
	public static RawDataFilePrefix getPrefixFor(final File dataFile) {
		if (dataFile == null)
			throw new IllegalArgumentException(I18N.err(44, "dataFile"));

		final RawDataFilePrefix prefixInfo = new RawDataFilePrefix();
		prefixInfo.read(dataFile);

		return prefixInfo;
	}

	/**
	 * Obtains the corresponding run description for the passed raw file prefix.
	 * 
	 * @param prefixInfo
	 *            a well-formed raw file prefix.
	 * @return a run description.
	 * @throws IllegalStateException
	 *             if the prefix is not well-formed.
	 * @throws IllegalArgumentException
	 *             if the prefix is {@code null}.
	 */
	public static RunDescription getRunDescriptionFor(
			final RawDataFilePrefix prefixInfo) {
		if (prefixInfo == null)
			throw new IllegalArgumentException(I18N.err(44, "prefixInfo"));

		if (prefixInfo.isWellFormed()) {

			final Timestamp started = new Timestamp(prefixInfo
					.getWallClockTime().getTime());
			final RunDescription run = new RunDescription(prefixInfo.getName(),
					prefixInfo.getRawDataVersion(), prefixInfo.getUserName(),
					prefixInfo.getJavaVersion(), prefixInfo.getJavaVendor(),
					prefixInfo.getOSName(), prefixInfo.getOSArch(), prefixInfo
							.getOSVersion(), prefixInfo.getMaxMemoryMb(),
					prefixInfo.getProcessors(), started);

			return run;
		} else {
			throw new IllegalStateException(I18N.err(107, prefixInfo.getFile()
					.getAbsolutePath()));
		}
	}

	/**
	 * Obtains the corresponding raw file handles for the passed raw file
	 * prefix.
	 * 
	 * @param prefixInfo
	 *            a well-formed raw file prefix.
	 * @return the corresponding raw file handles for the passed raw file
	 *         prefix.
	 * @throws IllegalStateException
	 *             if the prefix is not well-formed.
	 * @throws IllegalArgumentException
	 *             if the prefix is {@code null}.
	 */
	public static RawFileHandles getRawFileHandlesFor(
			final RawDataFilePrefix prefixInfo) {
		if (prefixInfo == null)
			throw new IllegalArgumentException(I18N.err(44, "prefixInfo"));

		if (prefixInfo.isWellFormed()) {
			String fileNamePrefix = prefixInfo.getFile().getAbsolutePath();
			for(String suffix : suffixes) {
				if (fileNamePrefix.endsWith(suffix)) {
					fileNamePrefix = fileNamePrefix.substring(0, fileNamePrefix
							.length()
							- suffix.length());
					break;
				} 
			}
			final File logFile = new File(fileNamePrefix + ".flog");
			if (!logFile.exists()) {
				SLLogger.getLogger().log(Level.WARNING,
						I18N.err(108, prefixInfo.getFile().getAbsolutePath()));
			}
			final RawFileHandles handles = new RawFileHandles(prefixInfo
					.getFile(), logFile);
			return handles;
		} else {
			throw new IllegalStateException(I18N.err(107, prefixInfo.getFile()
					.getAbsolutePath()));
		}
	}

	/**
	 * Used to get all the run descriptions and file handles in the Flashlight
	 * data directory.
	 */
	private static final class RawDataDirectoryReader {
		final Set<RunDescription> f_runs = new HashSet<RunDescription>();

    private final Map<RunDescription, RunDirectory> f_runToHandles =
        new HashMap<RunDescription, RunDirectory>();

		Set<RunDescription> getRunDescriptions() {
			return f_runs;
		}

		RunDirectory getRunDirectoryFor(final RunDescription description) {
			return f_runToHandles.get(description);
		}

		void read() {
	    final File directory = new File(FileUtility.getFlashlightDataDirectory());
	    final File[] runDirs = directory.listFiles(f_directoryFilter);
	    for (final File runDir : runDirs) {
	      final RunDirectory runDirectory = RunDirectory.getFor(runDir);
	      if (runDirectory != null) {
  	      final RunDescription run = runDirectory.getRunDescription();
  	      f_runs.add(run);
  	      f_runToHandles.put(run, runDirectory);
	      }
	    }
		}
	}

	/**
	 * @see #getRawDataFiles()
	 */
	private static final FileFilter f_flashlightRawDataFileFilter = new FileFilter() {
		public boolean accept(File pathname) {
			if (pathname.isDirectory()) {
				return false;
			}
			final String name = pathname.getName();			
			for(String suffix : suffixes) {
				if (name.endsWith(suffix)) {
					return true;
				}
			}
			return false;
		}
	};
	
	private static final FilenameFilter f_directoryFilter = new FilenameFilter() {
    public boolean accept(final File dir, final String name) {
      return (new File(dir, name).isDirectory()) &&
              name.contains("-at-") && !name.equals(DB_DIRECTORY);
    }
	};

	/**
	 * Find the raw data files.  These are going to located in directories nested
	 * in the flashlight data directory.  That is, each run produced by the
	 * "Run as Flashlight" option in Eclipse is going to create a top-level
	 * directory within the Flashlight data directory that contains all information
	 * needed for that run.
	 *  
	 * @return the set of Flashlight raw data files (compressed or uncompressed)
	 *         found in the Flashlight directory.
	 */
	private static File[] getRawDataFiles() {
		final File directory = new File(FileUtility.getFlashlightDataDirectory());
		
		final List<File> rawDataFiles = new LinkedList<File>(); 
		
		// Get the top-level per-run directories
		final File[] runDirs = directory.listFiles(f_directoryFilter);
		for (final File runDir : runDirs) {
	    final File[] dataFiles = runDir.listFiles(f_flashlightRawDataFileFilter);
	    rawDataFiles.addAll(Arrays.asList(dataFiles));
		}
		final File[] value = new File[rawDataFiles.size()];
		return rawDataFiles.toArray(value);
	}

	/*
	 * Estimate the amount of events in the raw file based upon the size of
	 * the raw file. This guess is only used for the pre-scan of the file.
	 */
	public static int estimateNumEvents(File dataFile) {
		final long sizeInBytes = dataFile.length();
		long estimatedEvents = (sizeInBytes / (RawFileUtility.isRawFileGzip(dataFile) ? 7L : 130L));
		if (estimatedEvents <= 0) {
			estimatedEvents = 10L;
		}
		return SLUtility.safeLongToInt(estimatedEvents);
	}
	
	public static SAXParser getParser(File dataFile) throws ParserConfigurationException, SAXException {
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		return isBinary(dataFile) ? new BinaryEventReader() : factory.newSAXParser();
	}
	
	private RawFileUtility() {
		// no instances
	}
}
