package com.surelogic.flashlight.common.jobs;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.surelogic.common.SLUtility;
import com.surelogic.common.derby.DerbyConnection;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBTransaction;
import com.surelogic.common.jdbc.QB;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.SubSLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.entities.PrepRunDescription;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.RawDataFilePrefix;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockAcquisition;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockRelease;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockWait;
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
import com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter;
import com.surelogic.flashlight.common.prep.LockSetAnalysis;
import com.surelogic.flashlight.common.prep.ObjectDefinition;
import com.surelogic.flashlight.common.prep.ReadWriteLock;
import com.surelogic.flashlight.common.prep.ScanRawFilePreScan;
import com.surelogic.flashlight.common.prep.ScanRawFilePrepScan;
import com.surelogic.flashlight.common.prep.ThreadDefinition;

public final class PrepSLJob extends AbstractSLJob {

	private static final int PRE_SCAN_WORK = 100;
	private static final int PERSIST_RUN_DESCRIPTION_WORK = 5;
	private static final int SETUP_WORK = 10;
	private static final int PREP_WORK = 200;
	private static final int FLUSH_WORK = 10;
	private static final int THREAD_LOCAL_FIELD_DELETE_WORK = 20;
	private static final int THREAD_LOCAL_OBJECT_DELETE_WORK = 20;
	private static final int EACH_POST_PREP = 30;

	private IPrep[] getParseHandlers(final IntrinsicLockDurationRowInserter i) {
		final BeforeTrace beforeTrace = new BeforeTrace(i);
		return new IPrep[] { beforeTrace, new AfterTrace(beforeTrace, i),
				new AfterIntrinsicLockAcquisition(beforeTrace, i),
				new AfterIntrinsicLockWait(beforeTrace, i),
				new AfterIntrinsicLockRelease(beforeTrace, i),
				new BeforeIntrinsicLockAcquisition(beforeTrace, i),
				new BeforeIntrinsicLockWait(beforeTrace, i),
				new BeforeUtilConcurrentLockAquisitionAttempt(beforeTrace, i),
				new AfterUtilConcurrentLockAcquisitionAttempt(beforeTrace, i),
				new AfterUtilConcurrentLockReleaseAttempt(beforeTrace, i),
				new ReadWriteLock(i), new ClassDefinition(),
				new FieldDefinition(), new FieldRead(beforeTrace, i),
				new FieldWrite(beforeTrace, i), new ObjectDefinition(),
				new ThreadDefinition() };
	}

	private IPostPrep[] getPostPrep() {
		return new IPostPrep[] { new LockSetAnalysis() };
	}

	private final File f_dataFile;
	private final DerbyConnection f_database;

	/**
	 * Constructs a job instance to prepare the passed raw data file into the
	 * passed target database.
	 * 
	 * @param dataFile
	 *            a raw data file, either <tt>.fl</tt> or <tt>.fl.gz</tt>.
	 * @param database
	 *            a derby connection object for the target database.
	 */
	public PrepSLJob(final File dataFile, final DerbyConnection database) {
		super("Preparing " + dataFile.getName());
		f_dataFile = dataFile;
		f_database = database;
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		final String dataFileName = f_dataFile.getName();

		final IPostPrep[] postPrepWork = getPostPrep();

		monitor.begin(PRE_SCAN_WORK + PERSIST_RUN_DESCRIPTION_WORK + SETUP_WORK
				+ PREP_WORK + FLUSH_WORK + THREAD_LOCAL_FIELD_DELETE_WORK
				+ THREAD_LOCAL_OBJECT_DELETE_WORK
				+ (EACH_POST_PREP * postPrepWork.length));
		/*
		 * Estimate the amount of events in the raw file based upon the size of
		 * the raw file. This guess is only used for the pre-scan of the file.
		 */
		final long sizeInBytes = f_dataFile.length();
		long estimatedEvents = (sizeInBytes / (RawFileUtility
				.isRawFileGzip(f_dataFile) ? 7L : 130L));
		if (estimatedEvents <= 0) {
			estimatedEvents = 10L;
		}
		final int estEventsInRawFile = SLUtility.safeLongToInt(estimatedEvents);
		try {
			final RawDataFilePrefix rawFilePrefix = RawFileUtility
					.getPrefixFor(f_dataFile);
			final RunDescription runDescription = RawFileUtility
					.getRunDescriptionFor(rawFilePrefix);
			final InputStream stream = RawFileUtility
					.getInputStreamFor(f_dataFile);
			try {
				/*
				 * Scan the file to collect the set of fields that were observed
				 * to be single-threaded. This information allows us to avoid
				 * inserting unnecessary data into the database.
				 */
				final SAXParserFactory factory = SAXParserFactory.newInstance();
				final SLProgressMonitor preScanMonitor = new SubSLProgressMonitor(
						monitor, "Pre-scanning the raw file", PRE_SCAN_WORK);
				preScanMonitor.begin(estEventsInRawFile);
				final ScanRawFilePreScan scanResults = new ScanRawFilePreScan(
						preScanMonitor);
				final SAXParser saxParser = factory.newSAXParser();
				saxParser.parse(stream, scanResults);
				preScanMonitor.done();
				stream.close();

				final int eventsInRawFile = SLUtility.safeLongToInt(scanResults
						.getElementCount());

				if (monitor.isCanceled()) {
					return SLStatus.CANCEL_STATUS;
				}

				/*
				 * Read the data file (our second pass) and insert prepared data
				 * into the database.
				 */
				final InputStream dataFileStream = RawFileUtility
						.getInputStreamFor(f_dataFile);
				return f_database
						.withTransaction(new DBTransaction<SLStatus>() {
							public SLStatus perform(final Connection c)
									throws Exception {

								/*
								 * Persist the run and obtain its database
								 * identifier, start time stamp, and the start
								 * time in nanoseconds.
								 */
								final SLProgressMonitor persistRunDescriptionMonitor = new SubSLProgressMonitor(
										monitor,
										"Persist the new run description",
										PERSIST_RUN_DESCRIPTION_WORK);
								persistRunDescriptionMonitor.begin();
								final Timestamp start = new Timestamp(
										rawFilePrefix.getWallClockTime()
												.getTime());
								final long startNS = rawFilePrefix
										.getNanoTime();
								final PrepRunDescription newRun = RunDAO
										.create(c, runDescription);
								final int runId = newRun.getRun();
								persistRunDescriptionMonitor.done();

								if (monitor.isCanceled()) {
									return SLStatus.CANCEL_STATUS;
								}

								/*
								 * Do the second pass through the file. This
								 * time we populate the database.
								 */
								final Set<Long> unreferencedObjects = new HashSet<Long>();
								final Set<Long> unreferencedFields = new HashSet<Long>();
								final IPrep[] f_elements = getParseHandlers(new IntrinsicLockDurationRowInserter(
										c));
								final SLProgressMonitor setupMonitor = new SubSLProgressMonitor(
										monitor, "Setting up event handlers",
										SETUP_WORK);
								setupMonitor.begin(f_elements.length);
								for (final IPrep element : f_elements) {
									element.setup(c, start, startNS,
											scanResults, unreferencedObjects,
											unreferencedFields);
									setupMonitor.worked(1);
								}
								setupMonitor.done();

								if (monitor.isCanceled()) {
									return SLStatus.CANCEL_STATUS;
								}

								final SLProgressMonitor prepMonitor = new SubSLProgressMonitor(
										monitor, "Preparing the raw file",
										PREP_WORK);
								prepMonitor.begin(eventsInRawFile);
								final ScanRawFilePrepScan handler = new ScanRawFilePrepScan(
										runId, c, prepMonitor, f_elements);
								saxParser.parse(dataFileStream, handler);
								prepMonitor.done();

								if (monitor.isCanceled()) {
									return SLStatus.CANCEL_STATUS;
								}

								final SLProgressMonitor flushMonitor = new SubSLProgressMonitor(
										monitor,
										"Flushing prepared data into the database",
										FLUSH_WORK);
								flushMonitor.begin(f_elements.length);
								for (final IPrep element : f_elements) {
									element.flush(runId, scanResults
											.getEndNanoTime());
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
										monitor,
										"Deleting thread-local fields",
										THREAD_LOCAL_FIELD_DELETE_WORK);
								threadLocalFieldDeleteMonitor.begin();
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
										monitor,
										"Deleting thread-local objects",
										THREAD_LOCAL_OBJECT_DELETE_WORK);
								threadLocalObjectDeleteMonitor.begin(

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

								for (final IPostPrep postPrep : postPrepWork) {
									final SLProgressMonitor postPrepMonitor = new SubSLProgressMonitor(
											monitor, postPrep.getDescription(),
											EACH_POST_PREP);
									postPrepMonitor.begin();
									postPrep.doPostPrep(c, runId);
									postPrepMonitor.done();
								}
								return SLStatus.OK_STATUS;
							}
						});
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
	}
}
