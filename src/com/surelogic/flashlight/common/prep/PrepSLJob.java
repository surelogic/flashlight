package com.surelogic.flashlight.common.prep;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.SLProgressMonitor;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.QB;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.common.entities.Run;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.Raw;

public final class PrepSLJob implements SLJob {

	private static final BeforeTrace beforeTrace = new BeforeTrace();

	/**
	 * New elements need to be added into this array.
	 */
	private static final IPrep[] f_elements = { beforeTrace,
			new AfterTrace(beforeTrace),
			new AfterIntrinisicLockAcquisition(beforeTrace),
			new AfterIntrinsicLockWait(beforeTrace),
			new AfterIntrinsisLockRelease(beforeTrace),
			new BeforeIntrinsicLockAcquisition(beforeTrace),
			new BeforeIntrinsicLockWait(beforeTrace),
			new BeforeUtilConcurrentLockAquisitionAttempt(beforeTrace),
			new AfterUtilConcurrentLockAcquisitionAttempt(beforeTrace),
			new AfterUtilConcurrentLockReleaseAttempt(beforeTrace),
			new ReadWriteLock(), new ClassDefinition(), new FieldDefinition(),
			new FieldRead(beforeTrace), new FieldWrite(beforeTrace),
			new ObjectDefinition(), new ThreadDefinition() };

	final Raw f_raw;

	public PrepSLJob(final Raw raw) {
		assert raw != null;
		f_raw = raw;
	}

	private InputStream getDataFileStream(final Raw raw) throws IOException {
		InputStream stream = new FileInputStream(raw.getDataFile());
		if (raw.isDataFileGzip()) {
			stream = new GZIPInputStream(stream, 32 * 1024);
		}
		return stream;
	}

	public SLStatus run(SLProgressMonitor monitor) {
		final String dataFileName = f_raw.getDataFile().getName();
		/*
		 * Estimate the work based upon the size of the raw file. This is only a
		 * guess and we will probably be a bit high.
		 */
		final long sizeInBytes = f_raw.getDataFile().length();
		long estimatedEvents = (sizeInBytes / (f_raw.isDataFileGzip() ? 7L
				: 130L));
		if (estimatedEvents <= 0) {
			estimatedEvents = 10L;
		}
		monitor.beginTask("Creating run information " + dataFileName, 4);
		try {
			InputStream stream = getDataFileStream(f_raw);
			monitor.worked(1);
			try {
				/*
				 * Scan the file to collect the set of fields that were observed
				 * to be single-threaded. This information allows us to avoid
				 * inserting unnecessary data into the database.
				 */
				final SAXParserFactory factory = SAXParserFactory.newInstance();
				final DataPreScan scanResults = new DataPreScan(monitor,
						estimatedEvents, dataFileName);
				SAXParser saxParser = factory.newSAXParser();
				final long startPreScan = System.currentTimeMillis();
				saxParser.parse(stream, scanResults);
				scanResults.done();

				System.out.println("Prescan = "
						+ (System.currentTimeMillis() - startPreScan) + " ms");
				stream.close();
				if (monitor.isCanceled()) {
					return SLStatus.CANCEL_STATUS;
				}
				/*
				 * Read the data file (our second pass) and insert all data into
				 * the database.
				 */
				stream = getDataFileStream(f_raw);
				try {
					// FIX change to decouple DB inserts?
					final Connection c = Data.getInstance().getConnection();
					c.setAutoCommit(false);
					/*
					 * Persist the run and obtain its database identifier, start
					 * timestamp, and the start time in nanoseconds.
					 */
					final Timestamp start;
					final long startNS;
					monitor.worked(1);
					start = new Timestamp(f_raw.getWallClockTime().getTime());
					startNS = f_raw.getNanoTime();

					final Run newRun = RunDAO.create(c, f_raw.getName(), f_raw
							.getRawDataVersion(), f_raw.getUserName(), f_raw
							.getJavaVersion(), f_raw.getJavaVendor(), f_raw
							.getOSName(), f_raw.getOSArch(), f_raw
							.getOSVersion(), f_raw.getMaxMemoryMB(), f_raw
							.getProcessors(), start);
					final int runId = newRun.getRun();
					/*
					 * Do the second pass through the file.
					 */
					final Set<Long> unreferencedObjects = new HashSet<Long>();
					final Set<Long> unreferencedFields = new HashSet<Long>();
					try {
						for (final IPrep element : f_elements) {
							element.setup(c, start, startNS, scanResults,
									unreferencedObjects, unreferencedFields);
						}
						final RawFileReader handler = new RawFileReader(runId,
								c, monitor, scanResults.getElementCount(),
								dataFileName);
						saxParser = factory.newSAXParser();
						final long startScan = System.currentTimeMillis();
						saxParser.parse(stream, handler);
						c.commit();
						System.out.println("Scan = "
								+ (System.currentTimeMillis() - startScan)
								+ " ms");

						for (final IPrep element : f_elements) {
							element.flush(runId, scanResults.getEndTime());
						}
						for (final IPrep element : f_elements) {
							element.printStats();
						}

						if (monitor.isCanceled()) {
							return SLStatus.CANCEL_STATUS;
						}
						/*
						 * Remove all unreferenced objects and fields.
						 */
						monitor.beginTask(
								"Deleting thread-local object information", 2);
						monitor.subTask("Deleting thread-local fields");
						System.out.printf(
								"Unreferenced fields calculated: %d\n",
								unreferencedFields.size());
						final PreparedStatement deleteFields = c
								.prepareStatement(QB.get(18));
						for (int i = 1; i <= 3; i++) {
							deleteFields.setInt(i, runId);
						}
						System.out.printf("Unreference fields deleted: %d\n",
								deleteFields.executeUpdate());
						monitor.worked(1);
						deleteFields.close();

						if (monitor.isCanceled()) {
							return SLStatus.CANCEL_STATUS;
						}
						monitor.subTask("Deleting thread-local objects");

						final PreparedStatement s = c.prepareStatement(QB
								.get(19));
						for (final Long l : unreferencedObjects) {
							s.setInt(1, runId);
							s.setLong(2, l);
							s.executeUpdate();
							monitor.worked(1);
						}
						s.close();
						System.out.printf("Unreference objects: %d\n",
								unreferencedObjects.size());
						monitor.worked(1);
						c.commit();
						monitor.beginTask("Performing lock set analysis", 1);
						LockSetAnalysis.analyze(c, runId);
						monitor.worked(1);
					} catch (final SQLException e) {
						e.printStackTrace(System.err);
						return SLStatus.createErrorStatus(0,
								"Could not work with the embedded database", e);
					} finally {
						c.commit();
						for (final IPrep element : f_elements) {
							element.close();
						}
						c.close();
					}
				} catch (final SQLException e) {
					e.printStackTrace(System.err);
					return SLStatus.createErrorStatus(0,
							"Could not work with the embedded database", e);
				}
			} finally {
				stream.close();
			}
		} catch (final Exception e) {
			if (monitor.isCanceled()) {
				return SLStatus.CANCEL_STATUS;
			}
			return SLStatus.createErrorStatus(0, "Unable to prepare "
					+ dataFileName, e);
		}
		return SLStatus.OK_STATUS;
	}

	public static class RawFileReader extends DefaultHandler {

		int f_work = 0;
		final int f_tickSize;
		final Connection f_c;
		final SLProgressMonitor f_monitor;
		final int f_run;
		final Set<String> f_notParsed = new HashSet<String>();

		public RawFileReader(final int run, final Connection c,
				final SLProgressMonitor monitor, final long eventCount,
				final String dataFileName) throws SQLException {
			f_run = run;
			f_c = c;
			f_monitor = monitor;
			int tickSize = 1;
			long work = eventCount;
			while (work > 500) {
				tickSize *= 10;
				work /= 10;
			}
			monitor.beginTask("Preparing file " + dataFileName, (int) work);
			f_tickSize = tickSize;
			f_notParsed.add("environment");
			f_notParsed.add("flashlight");
			f_notParsed.add("garbage-collected-object");
			f_notParsed.add("single-threaded-field");
			f_notParsed.add("time");
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			/*
			 * Show progress to the user
			 */
			if (f_work >= f_tickSize) {
				/*
				 * Check for cancel.
				 */
				if (f_monitor.isCanceled()) {
					throw new IllegalStateException("cancelled");
				}

				f_monitor.worked(1);
				f_work = 0;
				try {
					f_c.commit();
				} catch (final SQLException e) {
					SLLogger.getLogger().log(Level.SEVERE, "commit failed", e);
				}
			} else {
				f_work++;
			}

			boolean parsed = false;
			for (final IPrep element : f_elements) {
				if (name.equals(element.getXMLElementName())) {
					element.parse(f_run, attributes);
					parsed = true;
					break;
				}
			}

			if (!parsed && !f_notParsed.contains(name)) {
				final StringBuilder b = new StringBuilder();
				b.append(I18N.err(98, name));
				if (attributes == null || attributes.getLength() == 0) {
					b.append(" with no attributes.");
				} else {
					b.append(" with the following attributes:");
					for (int i = 0; i < attributes.getLength(); i++) {
						final String aName = attributes.getQName(i);
						final String aValue = attributes.getValue(i);
						b.append(" ").append(aName).append("=").append(aValue);
					}
					b.append(".");
				}
				SLLogger.getLogger().log(Level.WARNING, b.toString());
			}
		}
	}
}
