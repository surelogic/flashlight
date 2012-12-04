package com.surelogic.flashlight.common.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Holds file handles to the data file(s) and the log file of a Flashlight run
 * as well as time information.
 * 
 * @see RawFileUtility
 */
public final class RawFileHandles {

	RawFileHandles(@NonNull final RawDataFilePrefix[] data,
			@Nullable final File[] logs) {
		if (data == null)
			throw new IllegalArgumentException(I18N.err(44, "data"));
		if (data.length < 1)
			throw new IllegalArgumentException(I18N.err(92, "data"));

		f_data = new File[data.length];
		for (int j = 0; j < data.length; j++) {
			RawDataFilePrefix p = data[j];
			if (p == null)
				throw new IllegalArgumentException(I18N.err(44, "data[" + j
						+ "]"));
			f_data[j] = p.getFile();
		}
		/*
		 * The log file can be null.
		 */
		f_logs = logs == null ? new File[0] : logs;
	}

	/**
	 * Should contain at least one element.
	 */
	private final File[] f_data;

	public int numDataFiles() {
		return f_data.length;
	}

	public File getFirstDataFile() {
		return f_data[0];
	}

	/**
	 * A handle to the data files.
	 * 
	 * @return a non-null handle to the data files.
	 */
	public Iterable<File> getDataFiles() {
		return new Iterable<File>() {
			@Override
			public Iterator<File> iterator() {
				return new Iterator<File>() {
					int i = 0;

					@Override
					public boolean hasNext() {
						return i < f_data.length;
					}

					@Override
					public File next() {
						try {
							return f_data[i];
						} finally {
							i++;
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * Checks if the first known data file is compressed.
	 * 
	 * @return {@code true} if the data file is compressed, {@code false}
	 *         otherwise.
	 * 
	 * @see RawFileUtility#isRawFileGzip(File)
	 */
	public boolean isDataFileGzip() {
		return RawFileUtility.isRawFileGzip(getFirstDataFile());
	}

	@NonNull
	private final File[] f_logs;

	/**
	 * Gets all the log files found in the Flashlight directory. Do not mutate
	 * the returned array.
	 * 
	 * @return An array referencing all that log files found in the Flashlight
	 *         directory. May be empty.
	 */
	@NonNull
	public File[] getLogFiles() {
		return f_logs;
	}

	/**
	 * Checks the log of this raw file and reports if it is clean. A log file is
	 * clean if it doesn't contain the string <tt>!PROBLEM!</tt> within it.
	 * <tt>!PROBLEM!</tt> is the special string that the instrumentation uses to
	 * highlight a problem with.
	 * 
	 * @return {@code true} if the log file is clean or doesn't exist,
	 *         {@code false} otherwise.
	 */
	public boolean isLogClean() {
		boolean result = true;
		for (final File log : f_logs) {
			result &= isLogCleanHelper(log);
		}
		return result;
	}

	private boolean isLogCleanHelper(@NonNull File log) {
		try {
			if (log == null || !log.exists()) {
				return true;
			}
			final BufferedReader r = new BufferedReader(new FileReader(log));
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
			SLLogger.getLogger()
					.log(Level.SEVERE,
							I18N.err(40,
									log == null ? null : log.getAbsolutePath()),
							e);
		}
		return true;
	}

	public String getLogContentsAsAString() {
		final StringBuilder b = new StringBuilder();
		for (final File log : f_logs) {
			b.append("------------------------------------------------------\n");
			b.append("LOG FILE: ").append(log.getName()).append("\n\n");
			b.append("          ").append(log.getAbsolutePath()).append('\n');
			b.append("------------------------------------------------------\n\n");
			b.append(FileUtility.getFileContentsAsStringOrDefaultValue(log,
					"-empty-"));
			b.append("\n\n");
		}
		return b.toString();
	}
}
