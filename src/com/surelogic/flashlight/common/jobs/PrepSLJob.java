package com.surelogic.flashlight.common.jobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.QB;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLProgressUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.SubSLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.Data;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.RawFileHandles;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;
import com.surelogic.flashlight.common.prep.AfterIntrinisicLockAcquisition;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockWait;
import com.surelogic.flashlight.common.prep.AfterIntrinsisLockRelease;
import com.surelogic.flashlight.common.prep.AfterTrace;
import com.surelogic.flashlight.common.prep.AfterUtilConcurrentLockAcquisitionAttempt;
import com.surelogic.flashlight.common.prep.AfterUtilConcurrentLockReleaseAttempt;
import com.surelogic.flashlight.common.prep.BeforeIntrinsicLockAcquisition;
import com.surelogic.flashlight.common.prep.BeforeIntrinsicLockWait;
import com.surelogic.flashlight.common.prep.BeforeTrace;
import com.surelogic.flashlight.common.prep.BeforeUtilConcurrentLockAquisitionAttempt;
import com.surelogic.flashlight.common.prep.ClassDefinition;
import com.surelogic.flashlight.common.prep.FieldDefinition;
import com.surelogic.flashlight.common.prep.FieldRead;
import com.surelogic.flashlight.common.prep.FieldWrite;
import com.surelogic.flashlight.common.prep.IPostPrep;
import com.surelogic.flashlight.common.prep.IPrep;
import com.surelogic.flashlight.common.prep.LockSetAnalysis;
import com.surelogic.flashlight.common.prep.ObjectDefinition;
import com.surelogic.flashlight.common.prep.ReadWriteLock;
import com.surelogic.flashlight.common.prep.ScanRawFilePreScan;
import com.surelogic.flashlight.common.prep.ScanRawFilePrepScan;
import com.surelogic.flashlight.common.prep.ThreadDefinition;

public final class PrepSLJob implements SLJob {

	private static final int PRE_SCAN_WORK = 100;
	private static final int PERSIST_RUN_DESCRIPTION_WORK = 5;
	private static final int SETUP_WORK = 10;
	private static final int PREP_WORK = 200;
	private static final int FLUSH_WORK = 10;
	private static final int THREAD_LOCAL_FIELD_DELETE_WORK = 20;
	private static final int THREAD_LOCAL_OBJECT_DELETE_WORK = 20;
	private static final int EACH_POST_PREP = 30;

	private IPrep[] getParseHandlers() {
		final BeforeTrace beforeTrace = new BeforeTrace();
		return new IPrep[] { beforeTrace, new AfterTrace(beforeTrace),
				new AfterIntrinisicLockAcquisition(beforeTrace),
				new AfterIntrinsicLockWait(beforeTrace),
				new AfterIntrinsisLockRelease(beforeTrace),
				new BeforeIntrinsicLockAcquisition(beforeTrace),
				new BeforeIntrinsicLockWait(beforeTrace),
				new BeforeUtilConcurrentLockAquisitionAttempt(beforeTrace),
				new AfterUtilConcurrentLockAcquisitionAttempt(beforeTrace),
				new AfterUtilConcurrentLockReleaseAttempt(beforeTrace),
				new ReadWriteLock(), new ClassDefinition(),
				new FieldDefinition(), new FieldRead(beforeTrace),
				new FieldWrite(beforeTrace), new ObjectDefinition(),
				new ThreadDefinition() };
	}

	private IPostPrep[] getPostPrep() {
		return new IPostPrep[] { new LockSetAnalysis() };
	}

	private final RunDescription f_description;

	public PrepSLJob(final RunDescription description) {
		if (description == null)
			throw new IllegalArgumentException(I18N.err(44, "description"));
		f_description = description;
	}

	private InputStream getDataFileStream(final RawFileHandles handles)
			throws IOException {
		InputStream stream = new FileInputStream(handles.getDataFile());
		if (handles.isDataFileGzip()) {
			stream = new GZIPInputStream(stream, 32 * 1024);
		}
		return stream;
	}

	public SLStatus run(SLProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullSLProgressMonitor();

		final RawFileHandles handles = RawFileUtility
				.getRawFileHandlesFor(f_description);
		final File dataFile = handles.getDataFile();
		final String dataFileName = dataFile.getName();

		final IPostPrep[] postPrepWork = getPostPrep();

		monitor.beginTask("Preparing " + dataFileName, PRE_SCAN_WORK
				+ PERSIST_RUN_DESCRIPTION_WORK + SETUP_WORK + PREP_WORK
				+ FLUSH_WORK + THREAD_LOCAL_FIELD_DELETE_WORK
				+ THREAD_LOCAL_OBJECT_DELETE_WORK
				+ (EACH_POST_PREP * postPrepWork.length));
		/*
		 * Estimate the amount of events in the raw file based upon the size of
		 * the raw file. This guess is only used for the pre-scan of the file.
		 */
		final long sizeInBytes = dataFile.length();
		long estimatedEvents = (sizeInBytes / (handles.isDataFileGzip() ? 7L
				: 130L));
		if (estimatedEvents <= 0) {
			estimatedEvents = 10L;
		}
		int eventsInRawFile = SLProgressUtility.safeLongToInt(estimatedEvents);
		try {
			InputStream stream = getDataFileStream(handles);
			try {
				/*
				 * Scan the file to collect the set of fields that were observed
				 * to be single-threaded. This information allows us to avoid
				 * inserting unnecessary data into the database.
				 */
				final SAXParserFactory factory = SAXParserFactory.newInstance();
				final SLProgressMonitor preScanMonitor = new SubSLProgressMonitor(
						monitor, PRE_SCAN_WORK);
				preScanMonitor.beginTask("Pre-scanning the raw file",
						eventsInRawFile);
				final ScanRawFilePreScan scanResults = new ScanRawFilePreScan(
						preScanMonitor);
				SAXParser saxParser = factory.newSAXParser();
				saxParser.parse(stream, scanResults);
				preScanMonitor.done();
				stream.close();

				eventsInRawFile = SLProgressUtility.safeLongToInt(scanResults
						.getElementCount());

				if (monitor.isCanceled()) {
					return SLStatus.CANCEL_STATUS;
				}

				/*
				 * Read the data file (our second pass) and insert prepared data
				 * into the database.
				 */
				stream = getDataFileStream(handles);
				final Connection c = Data.getInstance().getConnection();
				try {
					c.setAutoCommit(false);

					/*
					 * Persist the run and obtain its database identifier, start
					 * timestamp, and the start time in nanoseconds.
					 */
					final SLProgressMonitor persistRunDescriptionMonitor = new SubSLProgressMonitor(
							monitor, PERSIST_RUN_DESCRIPTION_WORK);
					persistRunDescriptionMonitor.beginTask(
							"Persist the new run description", 1);
					final Timestamp start = new Timestamp(handles
							.getWallClockTime().getTime());
					final long startNS = handles.getNanoTime();
					final PrepRunDescription newRun = RunDAO.create(c,
							f_description);
					final int runId = newRun.getRun();
					persistRunDescriptionMonitor.done();

					if (monitor.isCanceled()) {
						return SLStatus.CANCEL_STATUS;
					}

					/*
					 * Do the second pass through the file. This time we
					 * populate the database.
					 */
					final Set<Long> unreferencedObjects = new HashSet<Long>();
					final Set<Long> unreferencedFields = new HashSet<Long>();
					final IPrep[] f_elements = getParseHandlers();
					final SLProgressMonitor setupMonitor = new SubSLProgressMonitor(
							monitor, SETUP_WORK);
					setupMonitor.beginTask("Setting up event handlers",
							f_elements.length);
					for (final IPrep element : f_elements) {
						element.setup(c, start, startNS, scanResults,
								unreferencedObjects, unreferencedFields);
						setupMonitor.worked(1);
					}
					setupMonitor.done();

					if (monitor.isCanceled()) {
						return SLStatus.CANCEL_STATUS;
					}

					final SLProgressMonitor prepMonitor = new SubSLProgressMonitor(
							monitor, PREP_WORK);
					prepMonitor.beginTask("Preparing the raw file",
							eventsInRawFile);
					final ScanRawFilePrepScan handler = new ScanRawFilePrepScan(
							runId, c, prepMonitor, f_elements);
					saxParser = factory.newSAXParser();
					saxParser.parse(stream, handler);
					c.commit();
					prepMonitor.done();

					if (monitor.isCanceled()) {
						return SLStatus.CANCEL_STATUS;
					}

					final SLProgressMonitor flushMonitor = new SubSLProgressMonitor(
							monitor, FLUSH_WORK);
					flushMonitor.beginTask(
							"Flushing prepared data into the database",
							f_elements.length);
					for (final IPrep element : f_elements) {
						element.flush(runId, scanResults.getEndNanoTime());
						flushMonitor.worked(1);
					}
					flushMonitor.done();

					if (monitor.isCanceled()) {
						return SLStatus.CANCEL_STATUS;
					}

					if (SLLogger.getLogger().isLoggable(Level.FINE)) {
						for (final IPrep element : f_elements) {
							element.printStats();
						}
					}

					if (monitor.isCanceled()) {
						return SLStatus.CANCEL_STATUS;
					}

					/*
					 * Remove all unreferenced objects and fields.
					 */
					final SLProgressMonitor threadLocalFieldDeleteMonitor = new SubSLProgressMonitor(
							monitor, THREAD_LOCAL_FIELD_DELETE_WORK);
					threadLocalFieldDeleteMonitor.beginTask(
							"Deleting thread-local fields", 1);
					final PreparedStatement deleteFields = c
							.prepareStatement(QB.get(18));
					for (int i = 1; i <= 3; i++) {
						deleteFields.setInt(i, runId);
					}
					deleteFields.executeUpdate();
					deleteFields.close();
					threadLocalFieldDeleteMonitor.done();

					if (monitor.isCanceled()) {
						return SLStatus.CANCEL_STATUS;
					}

					final SLProgressMonitor threadLocalObjectDeleteMonitor = new SubSLProgressMonitor(
							monitor, THREAD_LOCAL_OBJECT_DELETE_WORK);
					threadLocalObjectDeleteMonitor.beginTask(
							"Deleting thread-local objects",
							unreferencedObjects.size());
					final PreparedStatement deleteObjects = c
							.prepareStatement(QB.get(19));
					for (final Long l : unreferencedObjects) {
						deleteObjects.setInt(1, runId);
						deleteObjects.setLong(2, l);
						deleteObjects.executeUpdate();
						threadLocalObjectDeleteMonitor.worked(1);
					}
					deleteObjects.close();
					threadLocalObjectDeleteMonitor.done();

					c.commit();

					for (IPostPrep postPrep : postPrepWork) {
						final SLProgressMonitor postPrepMonitor = new SubSLProgressMonitor(
								monitor, EACH_POST_PREP);
						postPrepMonitor.beginTask(postPrep.getDescription(), 1);
						postPrep.doPostPrep(c, runId);
						postPrepMonitor.done();
					}
					c.commit();
				} finally {
					c.close();
				}
			} finally {
				stream.close();
			}
		} catch (final Exception e) {
			/*
			 * We check for a cancel here because a SAXException is thrown out
			 * of the parser when the user presses cancel.
			 */
			if (monitor.isCanceled()) {
				return SLStatus.CANCEL_STATUS;
			}
			final int code = 116;
			final String msg = I18N.err(code, dataFileName);
			return SLStatus.createErrorStatus(code, msg, e);
		} finally {
			RunManager.getInstance().refresh();
			monitor.done();
		}
		return SLStatus.OK_STATUS;
	}
}
