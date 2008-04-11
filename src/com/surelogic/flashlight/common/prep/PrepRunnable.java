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
import com.surelogic.common.logging.LogStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.common.entities.Run;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.Raw;

public final class PrepRunnable implements Runnable {
    Object status = null;
	
	/**
	 * New elements need to be added into this array.
	 */
	private static final IPrep[] f_elements = {
			new AfterIntrinisicLockAcquisition(), new AfterIntrinsicLockWait(),
			new AfterIntrinsisLockRelease(),
			new BeforeIntrinsicLockAcquisition(),
			new BeforeIntrinsicLockWait(), new ClassDefinition(),
			new FieldDefinition(), new FieldRead(), new FieldWrite(),
			new ObjectDefinition(), new ThreadDefinition() };

	final Raw f_raw;
    final SLProgressMonitor monitor;
	
	public PrepRunnable(final Raw raw, SLProgressMonitor mon) {
		assert raw != null;
		f_raw = raw;
		monitor = mon;
	}

	private InputStream getDataFileStream(final Raw raw) throws IOException {
		InputStream stream = new FileInputStream(raw.getDataFile());
		if (raw.isDataFileGzip()) {
			stream = new GZIPInputStream(stream);
		}
		return stream;
	}

	public void run() {
		final String dataFileName = f_raw.getDataFile().getName();
		/*
		 * Estimate the work based upon the size of the raw file. This is only a
		 * guess and we will probably be a bit high.
		 */
		long sizeInBytes = f_raw.getDataFile().length();
		long estimatedEvents = (sizeInBytes / (f_raw.isDataFileGzip() ? 7L
				: 130L));
		if (estimatedEvents <= 0)
			estimatedEvents = 10L;
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
				SAXParserFactory factory = SAXParserFactory.newInstance();
				final DataPreScan scanResults = 
					new DataPreScan(monitor,
						estimatedEvents, dataFileName);
				SAXParser saxParser = factory.newSAXParser();
				saxParser.parse(stream, scanResults);
				stream.close();
				if (monitor.isCanceled())
					return; //Status.CANCEL_STATUS;
				/*
				 * Read the data file (our second pass) and insert all data into
				 * the database.
				 */
				stream = getDataFileStream(f_raw);
				try {
					final Connection c = Data.getConnection();
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
					Set<Long> unreferencedObjects = new HashSet<Long>();
					Set<Long> unreferencedFields = new HashSet<Long>();
					try {
						for (IPrep element : f_elements) {
							element.setup(c, start, startNS, scanResults,
									unreferencedObjects, unreferencedFields);
						}
						RawFileReader handler = new RawFileReader(runId, c,
								monitor, scanResults.getElementCount(),
								dataFileName);
						saxParser = factory.newSAXParser();
						saxParser.parse(stream, handler);
						c.commit();
						if (monitor.isCanceled())
							return; // Status.CANCEL_STATUS;
						/*
						 * Remove all unreferenced objects and fields.
						 */
						monitor.beginTask(
								"Deleting thread-local object information",
								unreferencedFields.size()
										+ unreferencedObjects.size());
						monitor.subTask("Deleting thread-local fields");
						PreparedStatement s = c
								.prepareStatement("delete from FIELD where Run=? and Id=?");
						for (Long l : unreferencedFields) {
							s.setInt(1, runId);
							s.setLong(2, l);
							s.executeUpdate();
							monitor.worked(1);
						}
						s.close();
						if (monitor.isCanceled())
							return; // Status.CANCEL_STATUS;
						monitor.subTask("Deleting thread-local objects");
						s = c
								.prepareStatement("delete from OBJECT where Run=? and Id=?");
						for (Long l : unreferencedObjects) {
							s.setInt(1, runId);
							s.setLong(2, l);
							s.executeUpdate();
							monitor.worked(1);
						}
						s.close();
					} finally {
						c.commit();
						for (IPrep element : f_elements) {
							element.close();
						}
						c.close();
					}
				} catch (SQLException e) {					
					status = LogStatus.createErrorStatus(0,
							"Could not work with the embedded database", e);
					return;
				}
			} finally {
				stream.close();
			}
		} catch (Exception e) {
			if (monitor.isCanceled())
				return; // Status.CANCEL_STATUS;
			
			status = LogStatus.createErrorStatus(0,"Unable to prepare "
					+ dataFileName, e);
		    return;
		}
		//RunView.refreshViewContents();
		//monitor.done();
		//return Status.OK_STATUS;
	}

	public static class RawFileReader extends DefaultHandler {

		int f_work = 0;
		final int f_tickSize;
		final Connection f_c;
		final SLProgressMonitor f_monitor;
		final int f_run;

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
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			/*
			 * Check for cancel.
			 */
			if (f_monitor.isCanceled())
				throw new IllegalStateException("cancelled");
			/*
			 * Show progress to the user
			 */
			if (f_work >= f_tickSize) {
				f_monitor.worked(1);
				f_work = 0;
				try {
					f_c.commit();
				} catch (SQLException e) {
					SLLogger.getLogger().log(Level.SEVERE, "commit failed", e);
				}
			} else
				f_work++;

			for (IPrep element : f_elements) {
				if (name.equals(element.getXMLElementName())) {
					element.parse(f_run, attributes);
				}
			}
		}
	}
}
