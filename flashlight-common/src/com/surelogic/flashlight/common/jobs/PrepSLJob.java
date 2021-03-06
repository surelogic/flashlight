package com.surelogic.flashlight.common.jobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.SAXParser;

import com.carrotsearch.hppc.LongSet;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic._flashlight.common.OutputType;
import com.surelogic.common.SLUtility;
import com.surelogic.common.adhoc.AdHocQuery;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jdbc.DBConnection;
import com.surelogic.common.jdbc.NullDBTransaction;
import com.surelogic.common.jdbc.QB;
import com.surelogic.common.jdbc.SchemaUtility;
import com.surelogic.common.jdbc.TransactionException;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.SubSLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.model.FlashlightFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunDirectory;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockAcquisition;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockRelease;
import com.surelogic.flashlight.common.prep.AfterIntrinsicLockWait;
import com.surelogic.flashlight.common.prep.AfterUtilConcurrentLockAcquisitionAttempt;
import com.surelogic.flashlight.common.prep.AfterUtilConcurrentLockReleaseAttempt;
import com.surelogic.flashlight.common.prep.BeforeIntrinsicLockAcquisition;
import com.surelogic.flashlight.common.prep.BeforeIntrinsicLockWait;
import com.surelogic.flashlight.common.prep.BeforeUtilConcurrentLockAquisitionAttempt;
import com.surelogic.flashlight.common.prep.ClassDefinition;
import com.surelogic.flashlight.common.prep.ClassHierarchy;
import com.surelogic.flashlight.common.prep.EmptyQueries;
import com.surelogic.flashlight.common.prep.FieldAssignment;
import com.surelogic.flashlight.common.prep.FieldDefinition;
import com.surelogic.flashlight.common.prep.FieldRead;
import com.surelogic.flashlight.common.prep.FieldWrite;
import com.surelogic.flashlight.common.prep.HappensBeforeCollection;
import com.surelogic.flashlight.common.prep.HappensBeforeExecutor;
import com.surelogic.flashlight.common.prep.HappensBeforeObject;
import com.surelogic.flashlight.common.prep.HappensBeforePostPrep;
import com.surelogic.flashlight.common.prep.HappensBeforeThread;
import com.surelogic.flashlight.common.prep.IOneTimePrep;
import com.surelogic.flashlight.common.prep.IPostPrep;
import com.surelogic.flashlight.common.prep.IPrep;
import com.surelogic.flashlight.common.prep.IRangePrep;
import com.surelogic.flashlight.common.prep.IndirectAccess;
import com.surelogic.flashlight.common.prep.IntrinsicLockDurationRowInserter;
import com.surelogic.flashlight.common.prep.LockInfoPostPrep;
import com.surelogic.flashlight.common.prep.LockIsClassPostPrep;
import com.surelogic.flashlight.common.prep.LockSetAnalysis;
import com.surelogic.flashlight.common.prep.ObjectDefinition;
import com.surelogic.flashlight.common.prep.ReadWriteLock;
import com.surelogic.flashlight.common.prep.ScanRawFileFieldsPreScan;
import com.surelogic.flashlight.common.prep.ScanRawFilePreScan;
import com.surelogic.flashlight.common.prep.ScanRawFilePrepScan;
import com.surelogic.flashlight.common.prep.StaticCallLocation;
import com.surelogic.flashlight.common.prep.StaticFieldRead;
import com.surelogic.flashlight.common.prep.StaticFieldWrite;
import com.surelogic.flashlight.common.prep.ThreadDefinition;
import com.surelogic.flashlight.common.prep.ThreadNamePostPrep;
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

  IOneTimePrep[] getOneTimeHandlers(final IntrinsicLockDurationRowInserter i, final ClassHierarchy ch) {
    return new IOneTimePrep[] { new Trace(), new AfterIntrinsicLockAcquisition(i), new AfterIntrinsicLockWait(i),
        new AfterIntrinsicLockRelease(i), new BeforeIntrinsicLockAcquisition(i), new BeforeIntrinsicLockWait(i),
        new BeforeUtilConcurrentLockAquisitionAttempt(i), new AfterUtilConcurrentLockAcquisitionAttempt(i),
        new AfterUtilConcurrentLockReleaseAttempt(i), new ReadWriteLock(i), new StaticFieldRead(), new StaticFieldWrite(),
        new FieldDefinition(), new TraceNode(), new StaticCallLocation(), new ClassDefinition(), new HappensBeforeThread(ch),
        new HappensBeforeObject(ch), new HappensBeforeCollection(ch), new HappensBeforeExecutor(ch) };
  }

  IRangePrep[] getRangeHandlers() {
    return new IRangePrep[] { new FieldRead(), new FieldWrite(), new ObjectDefinition(), new ThreadDefinition(),
        new FieldAssignment(), new IndirectAccess() };
  }

  private IPostPrep[] getPostPrep(ClassHierarchy ch) {
    return new IPostPrep[] { new LockSetAnalysis(), new LockInfoPostPrep(), new HappensBeforePostPrep(ch), new ThreadNamePostPrep(),
        new LockIsClassPostPrep(), new EmptyQueries(f_runDirectory, f_queries) };
  }

  final RunDirectory f_runDirectory;
  final List<File> f_dataFiles;
  final Set<AdHocQuery> f_queries;
  final DBConnection f_database;
  final int f_windowSize;

  /**
   * Constructs a job instance that will prep the target run description.
   *
   * @param run
   * @param windowSize
   *          the number of receivers to scan at one time.
   * @param queries
   *          an optional set of queries that will be run and checked against
   *          results
   */
  public PrepSLJob(@NonNull final RunDirectory runDirectory, final int windowSize, @Nullable final Set<AdHocQuery> queries) {
    super(runDirectory.getPrepJobName());
    final File prepCompleteFile = FlashlightFileUtility.getPrepCompleteFileHandle(runDirectory.getDirectory());
    if (prepCompleteFile.exists()) {
      if (!prepCompleteFile.delete()) {
        /*
         * We just go on if the delete failed, but we do log an error about not
         * being able to delete the "complete" file.
         */
        SLLogger.getLogger().log(Level.SEVERE, I18N.err(167, prepCompleteFile.getAbsolutePath()));
      }
    }
    f_runDirectory = runDirectory;
    f_dataFiles = runDirectory.getRawFileHandles().getOrderedListOfCheckpointFiles();
    f_database = runDirectory.getDB();
    f_windowSize = windowSize;
    f_queries = queries != null ? queries : Collections.<AdHocQuery> emptySet();
  }

  @Override
  public SLStatus run(final SLProgressMonitor monitor) {
    final File runDir = f_runDirectory.getDirectory();
    int estEventsInRawFile = FlashlightFileUtility.estimateNumEvents(f_dataFiles);

    final ClassHierarchy ch = ClassHierarchy.load(new File(runDir, InstrumentationConstants.FL_CLASS_HIERARCHY_FILE_LOC),
        new File(runDir, InstrumentationConstants.FL_SITES_FILE_LOC),
        new File(runDir, InstrumentationConstants.FL_HAPPENS_BEFORE_FILE_LOC));

    final IPostPrep[] postPrepWork = getPostPrep(ch);

    monitor.begin(PRE_SCAN_WORK + DROP_CONSTRAINT_WORK + PERSIST_RUN_DESCRIPTION_WORK + SETUP_WORK + PREP_WORK * 2 + FLUSH_WORK
        + EACH_POST_PREP * postPrepWork.length + ADD_CONSTRAINT_WORK);

    File firstFile = f_dataFiles.get(0);

    try {

      if (monitor.isCanceled()) {
        return SLStatus.CANCEL_STATUS;
      }
      /*
       * Scan the file to collect the set of fields that were observed to be
       * single-threaded. This information allows us to avoid inserting
       * unnecessary data into the database.
       */

      final SLProgressMonitor preScanMonitor = new SubSLProgressMonitor(monitor, "Pre-scanning the raw file", PRE_SCAN_WORK);
      final ScanRawFilePreScan scanResults = new ScanRawFilePreScan(preScanMonitor);
      final SAXParser saxParser = OutputType.getParser(firstFile);
      for (File dataFile : f_dataFiles) {
        final InputStream stream = OutputType.getInputStreamFor(dataFile);
        preScanMonitor.begin(estEventsInRawFile);
        saxParser.parse(stream, scanResults);
        preScanMonitor.done();
        stream.close();
      }

      if (monitor.isCanceled()) {
        return SLStatus.CANCEL_STATUS;
      }

      final long eventsInRawFile = scanResults.getElementCount();
      f_database.destroy();
      f_database.withTransaction(new NullDBTransaction() {

        @Override
        public void doPerform(final Connection conn) throws Exception {
          /*
           * Persist the run and obtain its database identifier, start time
           * stamp, and the start time in nanoseconds.
           */
          final SLProgressMonitor persistRunDescriptionMonitor = new SubSLProgressMonitor(monitor,
              "Persist the new run description", PERSIST_RUN_DESCRIPTION_WORK);
          persistRunDescriptionMonitor.begin();
          final Timestamp start = f_runDirectory.getDescription().getStartTimeOfRun();
          final long startNS = f_runDirectory.getDescription().getCollectionDurationInNanos();
          saveRunDescription(conn);
          persistRunDescriptionMonitor.done();

          if (monitor.isCanceled()) {
            throw new CanceledException();
          }

          /*
           * Do the second pass through the file. This time we populate the
           * database.
           */
          final IntrinsicLockDurationRowInserter i = new IntrinsicLockDurationRowInserter(conn);

          final IOneTimePrep[] f_parseElements = getOneTimeHandlers(i, ch);
          final SLProgressMonitor setupMonitor = new SubSLProgressMonitor(monitor, "Setting up event handlers", SETUP_WORK);
          setupMonitor.begin(f_parseElements.length);
          for (final IOneTimePrep element : f_parseElements) {
            element.setup(conn, start, startNS, scanResults);
            setupMonitor.worked(1);
          }
          setupMonitor.done();

          if (monitor.isCanceled()) {
            throw new CanceledException();
          }

          final SLProgressMonitor prepMonitor = new SubSLProgressMonitor(monitor, "Preparing the raw file", PREP_WORK);
          prepMonitor.begin(SLUtility.safeLongToInt(eventsInRawFile));
          final ScanRawFilePrepScan parseHandler = new ScanRawFilePrepScan(conn, prepMonitor, f_parseElements);
          for (File dataFile : f_dataFiles) {
            final InputStream dataFileStream = OutputType.getInputStreamFor(dataFile);
            try {
              saxParser.parse(dataFileStream, parseHandler);
            } finally {
              dataFileStream.close();
            }
          }
          prepMonitor.done();
          if (monitor.isCanceled()) {
            throw new CanceledException();
          }

          final SLProgressMonitor flushMonitor = new SubSLProgressMonitor(monitor, "Flushing prepared data into the database",
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
          final SLProgressMonitor rprepMonitor = new SubSLProgressMonitor(monitor, "Preparing the raw file", PREP_WORK);
          final int numWindows = (int) (scanResults.getMaxReceiverId() / f_windowSize)
              + (scanResults.getMaxReceiverId() % f_windowSize > 0 ? 1 : 0);
          rprepMonitor.begin(SLUtility.safeLongToInt(eventsInRawFile / 16 * numWindows));
          final LongSet synthetics = scanResults.getSynthetics();
          final IRangePrep[] rpElements = getRangeHandlers();
          for (int j = 0; j < numWindows; j++) {
            final long begin = f_windowSize * j;
            final long end = f_windowSize * (j + 1) - 1;
            final ScanRawFileFieldsPreScan preScan = new ScanRawFileFieldsPreScan(rprepMonitor, synthetics, begin, end);

            for (File dataFile : f_dataFiles) {
              final InputStream infoStream = OutputType.getInputStreamFor(dataFile);
              try {
                saxParser.parse(infoStream, preScan);
              } finally {
                infoStream.close();
              }
            }
            for (final IRangePrep prep : rpElements) {
              prep.setup(conn, start, startNS, preScan, begin, end);
            }
            for (File dataFile : f_dataFiles) {
              final InputStream rangeStream = OutputType.getInputStreamFor(dataFile);
              try {
                final ScanRawFilePrepScan rangeHandler = new ScanRawFilePrepScan(conn, rprepMonitor, rpElements);
                saxParser.parse(rangeStream, rangeHandler);
              } finally {
                rangeStream.close();
              }
            }
            for (final IPrep prep : rpElements) {
              prep.flush(scanResults.getEndNanoTime());
            }
          }
          if (SLLogger.getLogger().isLoggable(Level.FINE)) {
            for (final IPrep element : f_parseElements) {
              element.printStats();
            }
          }
        }
      });

      final SLProgressMonitor constraintMonitor = new SubSLProgressMonitor(monitor, "Generating indexes", ADD_CONSTRAINT_WORK);
      final List<NullDBTransaction> constraints = addConstraints(constraintMonitor);
      constraintMonitor.begin(constraints.size());
      for (final NullDBTransaction constraint : constraints) {
        f_database.withTransaction(constraint);
        if (monitor.isCanceled()) {
          f_database.destroy();
          return SLStatus.CANCEL_STATUS;
        }
      }
      constraintMonitor.done();

      for (final IPostPrep postPrep : postPrepWork) {
        final SLProgressMonitor postPrepMonitor = new SubSLProgressMonitor(monitor, postPrep.getDescription(), EACH_POST_PREP);
        postPrepMonitor.begin();
        try {
          f_database.withTransaction(new NullDBTransaction() {
            @Override
            public void doPerform(final Connection conn) throws Exception {
              postPrep.doPostPrep(conn, f_database.getSchemaLoader(), monitor);
            }
          });
        } finally {
          postPrepMonitor.done();
        }
        if (monitor.isCanceled()) {
          f_database.destroy();
          return SLStatus.CANCEL_STATUS;
        }
      }

      FlashlightFileUtility.getPrepCompleteFileHandle(f_runDirectory.getDirectory()).createNewFile();
      return SLStatus.OK_STATUS;
    } catch (TransactionException e) {
      f_database.destroy();
      if (e.getCause() instanceof CanceledException) {
        return SLStatus.CANCEL_STATUS;
      } else {
        return SLStatus.createErrorStatus(116, runDir.getName(), e);
      }
    } catch (final Exception e) {
      /*
       * We check for a cancel here because a SAXException is thrown out of the
       * parser when the user presses cancel.
       */
      if (monitor.isCanceled()) {
        return SLStatus.CANCEL_STATUS;
      }
      final int code = 116;
      final String msg = I18N.err(code, runDir.getName());
      return SLStatus.createErrorStatus(code, msg, e);
    } finally {
      monitor.done();
    }

  }

  void saveRunDescription(final Connection c) throws SQLException {
    final RunDescription run = f_runDirectory.getDescription();
    final long durationNanos = run.getCollectionDurationInNanos();
    final PreparedStatement s = c.prepareStatement(QB.get("RunDAO.insert"));
    try {
      int i = 1;
      s.setString(i++, run.getName());
      s.setString(i++, run.getRawDataVersion());
      s.setString(i++, run.getHostname());
      s.setString(i++, run.getUserName());
      s.setString(i++, run.getJavaVersion());
      s.setString(i++, run.getJavaVendor());
      s.setString(i++, run.getOSName());
      s.setString(i++, run.getOSArch());
      s.setString(i++, run.getOSVersion());
      s.setInt(i++, run.getMaxMemoryMb());
      s.setInt(i++, run.getProcessors());
      s.setTimestamp(i++, run.getStartTimeOfRun());
      s.setLong(i++, durationNanos);
      s.executeUpdate();
    } finally {
      s.close();
    }
  }

  private List<NullDBTransaction> addConstraints(final SLProgressMonitor monitor) {
    final URL script = f_database.getSchemaLoader().getSchemaResource("add_constraints.sql");
    final List<NullDBTransaction> transactions = new ArrayList<>();
    try {
      for (final StringBuilder statement : SchemaUtility.getSQLStatements(script)) {
        transactions.add(new NullDBTransaction() {
          @Override
          public void doPerform(final Connection conn) throws Exception {
            try {
              SLLogger.getLoggerFor(PrepSLJob.class).fine(statement.toString());
              final Statement addSt = conn.createStatement();
              try {
                addSt.execute(statement.toString());
              } finally {
                addSt.close();
              }
            } catch (final SQLException e) {
              throw new IllegalStateException(I18N.err(12, statement.toString(), script), e);
            }
            monitor.worked(1);
          }
        });
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    return transactions;
  }

  static class CanceledException extends RuntimeException {
    private static final long serialVersionUID = -7858543475905909600L;
  }
}
