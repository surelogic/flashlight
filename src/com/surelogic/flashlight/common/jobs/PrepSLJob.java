package com.surelogic.flashlight.common.jobs;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;

import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.NullDBTransaction;
import com.surelogic.common.jdbc.SchemaUtility;
import com.surelogic.common.jdbc.TransactionException;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.SubSLProgressMonitor;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.UsageMeter;
import com.surelogic.flashlight.common.entities.RunDAO;
import com.surelogic.flashlight.common.files.RawDataFilePrefix;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunManager;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockAcquisition;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockRelease;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockWait;
import com.surelogic.flashlight.common.prep.AfterUtilConcurrentLockAcquisitionAttempt;
import com.surelogic.flashlight.common.prep.AfterUtilConcurrentLockReleaseAttempt;
import com.surelogic.flashlight.common.prep.BeforeIntrinsicLockAcquisition;
import com.surelogic.flashlight.common.prep.BeforeIntrinsicLockWait;
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
import com.surelogic.flashlight.common.prep.StaticCallLocation;
import com.surelogic.flashlight.common.prep.ThreadDefinition;
import com.surelogic.flashlight.common.prep.Trace;
import com.surelogic.flashlight.common.prep.TraceNode;

public final class PrepSLJob extends AbstractSLJob {

	private static final int PRE_SCAN_WORK = 100;
	private static final int DROP_CONSTRAINT_WORK = 5;
	private static final int PERSIST_RUN_DESCRIPTION_WORK = 5;
	private static final int SETUP_WORK = 10;
	private static final int PREP_WORK = 200;
	private static final int FLUSH_WORK = 10;
	private static final int EACH_POST_PREP = 50;
	private static final int ADD_CONSTRAINT_WORK = 100;

	private IPrep[] getParseHandlers(final IntrinsicLockDurationRowInserter i) {
		return new IPrep[] { new Trace(), new AfterIntrinsicLockAcquisition(i),
				new AfterIntrinsicLockWait(i),
				new AfterIntrinsicLockRelease(i),
				new BeforeIntrinsicLockAcquisition(i),
				new BeforeIntrinsicLockWait(i),
				new BeforeUtilConcurrentLockAquisitionAttempt(i),
				new AfterUtilConcurrentLockAcquisitionAttempt(i),
				new AfterUtilConcurrentLockReleaseAttempt(i), new FieldRead(i),
				new FieldWrite(i), new ReadWriteLock(i), new ClassDefinition(),
				new FieldDefinition(), new ThreadDefinition(), new TraceNode(),
				new StaticCallLocation(), new ObjectDefinition() };
	}

	private IPostPrep[] getPostPrep() {
		return new IPostPrep[] { new LockSetAnalysis() };
	}

	private final File f_dataFile;
	private final DBConnection f_database;

	public PrepSLJob(final RunDescription run) {
		super("Preparing " + run.getName());
		f_dataFile = run.getRawFileHandles().getDataFile();
		f_database = run.getDB();
	}

	/**
	 * Constructs a job instance to prepare the passed raw data file into the
	 * passed target database.
	 * 
	 * @param dataFile
	 *            a raw data file, either <tt>.fl</tt> or <tt>.fl.gz</tt>.
	 */
	public PrepSLJob(final File dataFile, final DBConnection database) {
		super("Preparing " + dataFile.getName());
		f_dataFile = dataFile;
		f_database = database;
	}

	public SLStatus run(final SLProgressMonitor monitor) {
		final String dataFileName = f_dataFile.getName();

		final IPostPrep[] postPrepWork = getPostPrep();

		monitor.begin(PRE_SCAN_WORK + DROP_CONSTRAINT_WORK
				+ PERSIST_RUN_DESCRIPTION_WORK + SETUP_WORK + PREP_WORK
				+ FLUSH_WORK + (EACH_POST_PREP * postPrepWork.length)
				+ ADD_CONSTRAINT_WORK);

		final SLStatus failed = SLLicenseUtility.validateSLJob(
				SLLicenseUtility.FLASHLIGHT_SUBJECT, monitor);
		if (failed != null) {
			return failed;
		}

		UsageMeter.getInstance().tickUse("Flashlight ran PrepSLJob");

		final int estEventsInRawFile = RawFileUtility
				.estimateNumEvents(f_dataFile);
		Exception exc = null;
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

				final SLProgressMonitor preScanMonitor = new SubSLProgressMonitor(
						monitor, "Pre-scanning the raw file", PRE_SCAN_WORK);
				preScanMonitor.begin(estEventsInRawFile);
				final ScanRawFilePreScan scanResults = new ScanRawFilePreScan(
						preScanMonitor);
				final SAXParser saxParser = RawFileUtility
						.getParser(f_dataFile);
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
				f_database.loggedBootAndCheckSchema();
				try {
					f_database.withTransaction(new NullDBTransaction() {
						@Override
						public void doPerform(final Connection c)
								throws Exception {

							/*
							 * Persist the run and obtain its database
							 * identifier, start time stamp, and the start time
							 * in nanoseconds.
							 */
							final SLProgressMonitor persistRunDescriptionMonitor = new SubSLProgressMonitor(
									monitor, "Persist the new run description",
									PERSIST_RUN_DESCRIPTION_WORK);
							persistRunDescriptionMonitor.begin();
							final Timestamp start = new Timestamp(rawFilePrefix
									.getWallClockTime().getTime());
							final long startNS = rawFilePrefix.getNanoTime();
							RunDAO.create(c, runDescription);
							persistRunDescriptionMonitor.done();

							if (monitor.isCanceled()) {
								throw new CanceledException();
							}

							/*
							 * Do the second pass through the file. This time we
							 * populate the database.
							 */
							final IntrinsicLockDurationRowInserter i = new IntrinsicLockDurationRowInserter(
									c);
							final IPrep[] f_parseElements = getParseHandlers(i);
							final SLProgressMonitor setupMonitor = new SubSLProgressMonitor(
									monitor, "Setting up event handlers",
									SETUP_WORK);
							setupMonitor.begin(f_parseElements.length);
							for (final IPrep element : f_parseElements) {
								element.setup(c, start, startNS, scanResults);
								setupMonitor.worked(1);
							}
							setupMonitor.done();

							if (monitor.isCanceled()) {
								throw new CanceledException();
							}

							final SLProgressMonitor prepMonitor = new SubSLProgressMonitor(
									monitor, "Preparing the raw file",
									PREP_WORK);
							prepMonitor.begin(eventsInRawFile);
							final ScanRawFilePrepScan parseHandler = new ScanRawFilePrepScan(
									c, prepMonitor, f_parseElements);
							final InputStream dataFileStream = RawFileUtility
									.getInputStreamFor(f_dataFile);
							try {
								saxParser.parse(dataFileStream, parseHandler);
							} finally {
								dataFileStream.close();
							}
							prepMonitor.done();

							if (monitor.isCanceled()) {
								throw new CanceledException();
							}

							final SLProgressMonitor flushMonitor = new SubSLProgressMonitor(
									monitor,
									"Flushing prepared data into the database",
									FLUSH_WORK);
							flushMonitor.begin(f_parseElements.length);
							for (final IPrep element : f_parseElements) {
								element.flush(scanResults.getEndNanoTime());
								flushMonitor.worked(1);
							}
							flushMonitor.done();

							if (monitor.isCanceled()) {
								throw new CanceledException();
							}

							if (SLLogger.getLogger().isLoggable(Level.FINE)) {
								for (final IPrep element : f_parseElements) {
									element.printStats();
								}
							}
							addConstraints(monitor).perform(c);
						}
					});
				} catch (final TransactionException e) {
					if (e.getCause() instanceof CanceledException) {
						f_database.destroy();
						return SLStatus.CANCEL_STATUS;
					} else {
						throw e;
					}
				}

				if (monitor.isCanceled()) {
					f_database.destroy();
					return SLStatus.CANCEL_STATUS;
				}
				f_database.withTransaction(new NullDBTransaction() {

					@Override
					public void doPerform(final Connection conn)
							throws Exception {
						for (final IPostPrep postPrep : postPrepWork) {
							final SLProgressMonitor postPrepMonitor = new SubSLProgressMonitor(
									monitor, postPrep.getDescription(),
									EACH_POST_PREP);
							postPrepMonitor.begin();
							postPrep.doPostPrep(conn);
							postPrepMonitor.done();
						}

					}
				});
				System.out.println(scanResults);
				return SLStatus.OK_STATUS;

			} finally {
				stream.close();
			}
		} catch (final Exception e) {
			exc = e;
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
			try {
				RunManager.getInstance().refresh();
			} catch (final RuntimeException t) {
				if (exc == null) {
					throw t;
				}
			}
			monitor.done();
		}
	}

	private NullDBTransaction addConstraints(final SLProgressMonitor monitor) {
		return new NullDBTransaction() {

			@Override
			public void doPerform(final Connection conn) throws Exception {
				final SLProgressMonitor constraintMonitor = new SubSLProgressMonitor(
						monitor, "Generating indexes", ADD_CONSTRAINT_WORK);
				final URL script = f_database.getSchemaLoader()
						.getSchemaResource("add_constraints.sql");
				final List<StringBuilder> statements = SchemaUtility
						.getSQLStatements(script);
				constraintMonitor.begin(statements.size());
				final Statement addSt = conn.createStatement();
				try {
					for (final StringBuilder statement : statements) {
						try {
							addSt.execute(statement.toString());
						} catch (final SQLException e) {
							throw new IllegalStateException(I18N.err(12,
									statement.toString(), script), e);
						}
						constraintMonitor.worked(1);
					}
				} finally {
					addSt.close();
				}
				constraintMonitor.done();
			}
		};
	}

	private static class CanceledException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7858543475905909600L;

	}
}
