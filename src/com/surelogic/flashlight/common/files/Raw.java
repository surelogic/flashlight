package com.surelogic.flashlight.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.entities.IRunDescription;

public final class Raw implements IRunDescription {
        public static final String SUFFIX = ".fl";
        public static final String COMPRESSED_SUFFIX = ".fl.gz";
    
	public static final String DEFAULT_RAW_LOCATION = 
		System.getProperty("user.home") + System.getProperty("file.separator") + "Flashlight";
	
	public static Raw[] findRawFiles(String location) {	
		Raw[] raws = new Raw.Builder().addDirectory(location).build();
		return raws;
	}
	
	public static Raw createRawFile(File loc) {
		return new Builder().build(loc);
	}
	
	private final File f_data;

	public File getDataFile() {
		return f_data;
	}

	private final boolean f_gzip;

	public boolean isDataFileGzip() {
		return f_gzip;
	}

	private final File f_log;

	public File getLogFile() {
		return f_log;
	}

	private final String f_name;

	public String getName() {
		return f_name;
	}

	private final String f_rawDataVersion;

	public String getRawDataVersion() {
		return f_rawDataVersion;
	}

	private final String f_userName;

	public String getUserName() {
		return f_userName;
	}

	private final String f_javaVersion;

	public String getJavaVersion() {
		return f_javaVersion;
	}

	private final String f_javaVendor;

	public String getJavaVendor() {
		return f_javaVendor;
	}

	private final String f_osName;

	public String getOSName() {
		return f_osName;
	}

	private final String f_osArch;

	public String getOSArch() {
		return f_osArch;
	}

	private final String f_osVersion;

	public String getOSVersion() {
		return f_osVersion;
	}

	private final int f_maxMemoryMB;

	public int getMaxMemoryMB() {
		return f_maxMemoryMB;
	}

	private final int f_processors;

	public int getProcessors() {
		return f_processors;
	}

	private final long f_nanoTime;

	public long getNanoTime() {
		return f_nanoTime;
	}

	private final Date f_wallClockTime;

	public Date getWallClockTime() {
		return f_wallClockTime;
	}

	private final Timestamp f_started;

	public Timestamp getStartTimeOfRun() {
		return f_started;
	}

	public boolean isSameRun(IRunDescription run) {
		if (getName().equals(run.getName())) {
			if (getStartTimeOfRun().equals(run.getStartTimeOfRun()))
				return true;
		}
		return false;
	}

	private Raw(final File data, final boolean gzip, final File log,
			final String name, final String rawDataVersion,
			final String userName, final String javaVersion,
			final String javaVendor, final String osName, final String osArch,
			final String osVersion, final int maxMemoryMB,
			final int processors, final long nanoTime, final Date wallClockTime) {
		assert data != null;
		f_data = data;
		f_gzip = gzip;
		assert log != null;
		f_log = log;
		assert name != null;
		f_name = name;
		assert rawDataVersion != null;
		f_rawDataVersion = rawDataVersion;
		assert userName != null;
		f_userName = userName;
		assert javaVersion != null;
		f_javaVersion = javaVersion;
		assert javaVendor != null;
		f_javaVendor = javaVendor;
		assert osName != null;
		f_osName = osName;
		assert osArch != null;
		f_osArch = osArch;
		assert osVersion != null;
		f_osVersion = osVersion;
		f_maxMemoryMB = maxMemoryMB;
		f_processors = processors;
		f_nanoTime = nanoTime;
		assert wallClockTime != null;
		f_wallClockTime = wallClockTime;
		f_started = new Timestamp(f_wallClockTime.getTime());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();		
		sb.append("Name:").append(f_name).append('\n');
		sb.append("Format: v").append(f_rawDataVersion).append('\n');
		sb.append("Data: ").append(f_data.getAbsolutePath());
		if (f_gzip) {
			sb.append(" (Compressed)\n");
		} else {
			sb.append('\n');
		}
		sb.append("Log: ").append(f_log.getAbsolutePath());
		sb.append("User: ").append(f_userName).append('\n');
		sb.append("Java: ").append(f_javaVendor).append(' ').append(f_javaVersion).append('\n');
		sb.append("OS: ").append(f_osName).append(' ').append(f_osArch).append(' ');
		sb.append(f_osVersion).append('\n');
		sb.append("Max Memory: ").append(f_maxMemoryMB).append(" MB\n");
		sb.append("CPUs: ").append(f_processors).append('\n');
		sb.append("Started at: ").append(f_started).append('\n');
		sb.append("Run Time: ").append(f_nanoTime).append(" ns\n");
		return sb.toString();
	}
	
	public static class Builder {

		private static class PrefixReader extends DefaultHandler {

			String name;

			String rawDataVersion;

			String userName;

			String javaVersion;

			String javaVendor;

			String osName;

			String osArch;

			String osVersion;

			int maxMemoryMB;

			int processors;

			long nanoTime;

			Date wallClockTime;

			boolean isWellFormed() {
				if (name == null)
					return false;
				if (rawDataVersion == null)
					return false;
				if (userName == null)
					return false;
				if (javaVersion == null)
					return false;
				if (javaVendor == null)
					return false;
				if (osName == null)
					return false;
				if (osArch == null)
					return false;
				if (osVersion == null)
					return false;
				if (maxMemoryMB == 0)
					return false;
				if (processors == 0)
					return false;
				if (nanoTime == 0)
					return false;
				if (wallClockTime == null)
					return false;
				return true;
			}

			@Override
			public void startElement(String uri, String localName, String name,
					Attributes attributes) throws SAXException {
				boolean isPrefixElement = name.equals("flashlight")
						|| name.equals("environment") || name.equals("time");
				if (!isPrefixElement) {
					/*
					 * Stop reading the file, we are past the information we
					 * want to read. This is a bit of a hack, but it is the only
					 * way to tell the parser to stop.
					 */
					throw new SAXException("done");
				}
				if (attributes != null) {
					for (int i = 0; i < attributes.getLength(); i++) {
						final String aName = attributes.getQName(i);
						final String aValue = attributes.getValue(i);
						if ("run".equals(aName)) {
							this.name = aValue;
						} else if ("version".equals(aName)) {
							rawDataVersion = aValue;
						} else if ("user-name".equals(aName)) {
							userName = aValue;
						} else if ("java-version".equals(aName)) {
							javaVersion = aValue;
						} else if ("java-vendor".equals(aName)) {
							javaVendor = aValue;
						} else if ("os-name".equals(aName)) {
							osName = aValue;
						} else if ("os-arch".equals(aName)) {
							osArch = aValue;
						} else if ("os-version".equals(aName)) {
							osVersion = aValue;
						} else if ("max-memory-mb".equals(aName)) {
							maxMemoryMB = Integer.parseInt(aValue);
						} else if ("processors".equals(aName)) {
							processors = Integer.parseInt(aValue);
						} else if ("nano-time".equals(aName)) {
							nanoTime = Long.parseLong(aValue);
						} else if ("wall-clock-time".equals(aName)) {
							final SimpleDateFormat dateFormat = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss.SSS");
							try {
								wallClockTime = dateFormat.parse(aValue);
							} catch (ParseException e) {
								throw new SAXException(e);
							}
						}
					}
				}
			}
		}

		final List<File> f_path = new ArrayList<File>();
		private final FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				final String name = pathname.getName();
				return name.endsWith(COMPRESSED_SUFFIX) || 
                       name.endsWith(SUFFIX);
			}
		};

		public Builder addDirectory(final String directory) {
			return addDirectory(new File(directory));
		}

		public Builder addDirectory(final File directory) {
			if (directory == null)
				throw new IllegalArgumentException("directory must be non-null");
			if (directory.isDirectory()) {
				f_path.add(directory);
			} else {
				throw new IllegalArgumentException(
						"directory must be a directory");
			}
			return this;
		}

		public Raw build(File loc) {
			if (filter.accept(loc)) {
				final List<Raw> result = new ArrayList<Raw>(1);
				try {
					add(loc, result);
					return result.get(0);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			return null;
		}
		
		public Raw[] build() {
			final ArrayList<Raw> result = new ArrayList<Raw>();
			for (File directory : f_path) {
				File[] dataFiles = directory.listFiles(filter);
				for (File f : dataFiles) {
					try {
						add(f, result);
					} catch (Exception e) {
						/*
						 * Things could go badly if file permissions are wrong,
						 * or someone deletes the file being examined.
						 */
						SLLogger
								.getLogger()
								.log(
										Level.WARNING,
										"Failure during examination of raw data files. "
												+ "Perhaps due to permissions or concurrent operations.",
										e);
					}
				}
			}
			return result.toArray(new Raw[result.size()]);
		}

		private void add(final File dataFile, final List<Raw> mutableResult)
				throws Exception {
			if (dataFile == null || !dataFile.exists()) {
				SLLogger.getLogger().log(
						Level.WARNING,
						"Data file "
								+ (dataFile == null ? "Unknown" : dataFile
										.getName()) + " does not exist.");
				return;
			}

			String prefix = dataFile.getAbsolutePath();
			final boolean gzip;
			final InputStream stream;
			if (dataFile.getName().endsWith(".fl")) {
				gzip = false;
				prefix = prefix.substring(0, prefix.length() - 3);
				stream = new FileInputStream(dataFile);
			} else {
				gzip = true;
				prefix = prefix.substring(0, prefix.length() - 6);
				stream = new GZIPInputStream(new FileInputStream(dataFile));
			}
			try {
				File logFile = new File(prefix + ".flog");
				if (!logFile.exists()) {
					SLLogger.getLogger()
							.log(
									Level.WARNING,
									"No log file found for "
											+ dataFile.getName() + ".");
					return;
				}

				/*
				 * Read the flashlight, environment, and time elements from the
				 * data file.
				 */
				SAXParserFactory factory = SAXParserFactory.newInstance();
				PrefixReader handler = new PrefixReader();
				try {
					// Parse the input
					SAXParser saxParser = factory.newSAXParser();
					saxParser.parse(stream, handler);
				} catch (SAXException e) {
					/*
					 * Ignore, this is expected because we don't want to parse
					 * the entire really-really huge file. However, make sure we
					 * got what we wanted.
					 */
					if (!e.getMessage().equals("done")) {
						SLLogger.getLogger().log(
								Level.WARNING,
								"XML parsing problem reading header of data file "
										+ dataFile.getName() + ".");
						throw new SAXException(e);
					}
				}

				/*
				 * If we got all the data we needed from the file go ahead and
				 * create a new instance.
				 */
				if (handler.isWellFormed()) {
					mutableResult.add(new Raw(dataFile, gzip, logFile,
							handler.name, handler.rawDataVersion,
							handler.userName, handler.javaVersion,
							handler.javaVendor, handler.osName, handler.osArch,
							handler.osVersion, handler.maxMemoryMB,
							handler.processors, handler.nanoTime,
							handler.wallClockTime));
				} else {
					SLLogger.getLogger().log(
							Level.WARNING,
							"Incomplete information read from "
									+ dataFile.getName() + ".");
				}
			} finally {
				stream.close();
			}
		}
	}

	/**
	 * Checks the log of this raw file and reports if it is clean. A log file is
	 * clean if it doesn't contain the string <tt>!PROBLEM!</tt> within it.
	 * <tt>!PROBLEM!</tt> is the special string that the instrumentation uses
	 * to highlight a problem with.
	 * 
	 * @return <code>true</code> if the log file is clean, <code>false</code>
	 *         otherwise.
	 */
	public boolean isLogClean() {
		try {
			BufferedReader r = new BufferedReader(new FileReader(f_log));
			try {
				while (true) {
					String s = r.readLine();
					if (s == null)
						break;
					if (s.indexOf("!PROBLEM!") != -1)
						return false;
				}
			} finally {
				r.close();
			}
		} catch (IOException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Unable to examine log file " + f_log.getAbsolutePath(), e);
		}
		return true;
	}
}
