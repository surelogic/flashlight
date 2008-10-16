package com.surelogic.flashlight.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Holds file handles to the data file and the log file of a Flashlight run as
 * well as time information.
 * 
 * @see RawFileUtility
 */
public final class RawFileHandles {

	RawFileHandles(final File data, final File log) {
		if (data == null)
			throw new IllegalArgumentException(I18N.err(44, "data"));
		f_data = data;
		/*
		 * The log file can be null.
		 */
		f_log = log;
	}

	private final File f_data;

	/**
	 * A handle to the data file.
	 * 
	 * @return a non-null handle to the data file.
	 */
	public File getDataFile() {
		return f_data;
	}

	/**
	 * Checks if the data file is compressed.
	 * 
	 * @return {@code true} if the data file is compressed, {@code false}
	 *         otherwise.
	 */
	public boolean isDataFileGzip() {
		return RawFileUtility.isRawFileGzip(f_data);
	}

	public boolean isDataFileBinary() {
		return RawFileUtility.isBinary(f_data);
	}
	
	private final File f_log;

	/**
	 * A handle to the log file if one exists.
	 * 
	 * @return a handle to the log file, or {@code null} if no log file exists.
	 */
	public File getLogFile() {
		return f_log;
	}

	/**
	 * Checks the log of this raw file and reports if it is clean. A log file is
	 * clean if it doesn't contain the string <tt>!PROBLEM!</tt> within it.
	 * <tt>!PROBLEM!</tt> is the special string that the instrumentation uses to
	 * highlight a problem with.
	 * 
	 * @return <code>true</code> if the log file is clean, <code>false</code>
	 *         otherwise.
	 */
	public boolean isLogClean() {
		try {
			final BufferedReader r = new BufferedReader(new FileReader(f_log));
			try {
				while (true) {
					final String s = r.readLine();
					if (s == null) {
						break;
					}
					if (s.indexOf("!PROBLEM!") != -1) {
						return false;
					}
				}
			} finally {
				r.close();
			}
		} catch (final Exception e) {
			SLLogger.getLogger().log(Level.SEVERE,
					I18N.err(40, f_log.getAbsolutePath()), e);
		}
		return true;
	}
}
