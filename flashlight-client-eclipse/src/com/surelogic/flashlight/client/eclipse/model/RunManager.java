package com.surelogic.flashlight.client.eclipse.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.eclipse.core.runtime.jobs.Job;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.core.animation.timing.TimingSource.TickListener;
import org.jdesktop.core.animation.timing.sources.ScheduledExecutorTimingSource;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.Singleton;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
import com.surelogic._flashlight.common.InstrumentationConstants;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.dialogs.RunControlDialog;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.views.adhoc.AdHocDataSource;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.model.FlashlightFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * A singleton that manages the state of the Flashlight data directory. This is
 * the hub to understand the state of a running instrumented program, as well as
 * to obtain handles to raw and prepared collected data.
 */
@ThreadSafe
@Singleton
@Region("private RunState")
@RegionLock("RunStateLock is f_lock protects RunState")
public final class RunManager implements ILifecycle {

  private static final RunManager INSTANCE = new RunManager();

  private static final AtomicBoolean f_firstUse = new AtomicBoolean(true);

  /**
   * Gets the singleton instance of the Flashlight run manager.
   * 
   * @return the singleton instance of the Flashlight run manager.
   */
  @NonNull
  public static RunManager getInstance() {
    if (f_firstUse.compareAndSet(true, false)) {
      // first use of the run manager
      INSTANCE.init();
    }
    return INSTANCE;
  }

  @Unique("return")
  @Vouch("No need to check utility call")
  private RunManager() {
    f_dataDir = EclipseUtility.getFlashlightDataDirectory();
  }

  private final TimingSource f_timer = new ScheduledExecutorTimingSource(5, TimeUnit.SECONDS);

  /**
   * Autmatically called by {@link #getInstance()} &mdash; client code should
   * not invoke.
   */
  @Override
  public void init() {
    f_timer.addTickListener(f_tick);
    f_timer.init();
  }

  @Override
  public void dispose() {
    f_timer.dispose();
  }

  @Vouch("AnnotationBounds")
  private final CopyOnWriteArraySet<IRunManagerObserver> f_observers = new CopyOnWriteArraySet<IRunManagerObserver>();

  public void addObserver(final IRunManagerObserver o) {
    if (o == null) {
      return;
    }
    f_observers.add(o);
  }

  public void removeObserver(final IRunManagerObserver o) {
    f_observers.remove(o);
  }

  /**
   * Do not call this method while holding a lock!
   */
  private void notifyCollectionCompletedRunDirectoryChange() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyCollectionCompletedRunDirectoryChange();
    }
  }

  /**
   * Do not call this method while holding a lock!
   */
  private void notifyLaunchedRunChange() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyLaunchedRunChange();
    }
  }

  /**
   * Do not call this method while holding a lock!
   */
  private void notifyPrepareDataJobScheduled() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyPrepareDataJobScheduled();
    }
  }

  /**
   * A reference to the Flashlight data directory.
   */
  @Vouch("ThreadSafe")
  private final File f_dataDir;

  /**
   * Gets an abstract representation of the Flashlight data directory.
   * 
   * @return an abstract representation of the Flashlight data directory.
   */
  @NonNull
  public File getDirectory() {
    return f_dataDir;
  }

  /**
   * Gets the run identity string for the passed a handle to a run directory on
   * the disk. The result is the same as invoking <tt>directory.getName()</tt>.
   * 
   * @param directory
   *          a directory.
   * @return the run identity string for the passed a handle to a run directory
   *         on the disk.
   */
  @NonNull
  public static String getRunIdStringFrom(@NonNull final File directory) {
    if (directory == null) {
      throw new IllegalArgumentException(I18N.err(44, "directory"));
    }
    return directory.getName();
  }

  /**
   * Gets an abstract representation of the run directory on the disk in the
   * Flashlight data directory. The result is the same as invoking
   * 
   * <pre>
   * new File(RunManager.getInstance().getDirectory(), runIdString)
   * </pre>
   * 
   * @param runIdString
   *          a run identity string.
   * @return an abstract representation of the run directory on the disk in the
   *         Flashlight data directory.
   * 
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  @NonNull
  public File getDirectoryFrom(@NonNull final String runIdString) {
    if (runIdString == null) {
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    }
    final File result = new File(getDirectory(), runIdString);
    return result;
  }

  /**
   * Lock used to protect mutable state managed by this class.
   */
  private final Object f_lock = new Object();

  /*
   * Sets that track launching, collection, and termination of an instrumented
   * program.
   * 
   * These use the run identity string because, except for the last set,
   * collection has not yet completed on these runs.
   */

  @UniqueInRegion("RunState")
  private final LinkedList<LaunchedRun> f_launchedRuns = new LinkedList<LaunchedRun>();

  /**
   * Gets an ordered list of runs launched during this Eclipse session. The list
   * is ordered from newest to oldest time of launch. The returned list is a
   * copy and may be freely mutated.
   * <p>
   * The items contained in the returned list should not be mutated.
   * 
   * @return an ordered list of runs launched during this Eclipse session.
   */
  @NonNull
  public ArrayList<LaunchedRun> getLaunchedRuns() {
    synchronized (f_lock) {
      return new ArrayList<LaunchedRun>(f_launchedRuns);
    }
  }

  /**
   * Gets the launched run for the passed run identity string, or {@code null}
   * if none.
   * 
   * @param runIdString
   *          a run identity string.
   * @return the launched run for the passed run identity string, or
   *         {@code null} if none.
   */
  @Nullable
  private LaunchedRun getLaunchedRunFor(@NonNull final String runIdString) {
    if (runIdString != null) {
      synchronized (f_lock) {
        for (LaunchedRun lrun : f_launchedRuns) {
          if (lrun.getRunIdString().equals(runIdString)) {
            return lrun;
          }
        }
      }
    }
    return null;
  }

  /**
   * Notifies the run manager that an application is being instrumented and
   * launched.
   * 
   * @param runIdString
   *          a run identity string.
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public void notifyPerformingInstrumentationAndLaunch(@NonNull final String runIdString) {
    if (runIdString == null) {
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    }
    synchronized (f_lock) {
      LaunchedRun lrun = getLaunchedRunFor(runIdString);
      if (lrun != null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(168, runIdString));
        return;
      }
      lrun = new LaunchedRun(runIdString);
      f_launchedRuns.addFirst(lrun);
    }
    notifyLaunchedRunChange();
    // TODO perhaps a better way with a preference
    RunControlDialog.show();
  }

  /**
   * Notifies the run manager that a launched run is collecting data.
   * 
   * @param runIdString
   *          a run identity string.
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public void notifyCollectingData(@NonNull final String runIdString) {
    if (runIdString == null) {
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    }
    synchronized (f_lock) {
      LaunchedRun lrun = getLaunchedRunFor(runIdString);
      if (lrun == null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(235, runIdString));
        lrun = new LaunchedRun(runIdString);
        f_launchedRuns.addFirst(lrun);
      }
      if (!lrun.setState(RunState.COLLECTING_DATA)) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(170, runIdString));
        return;
      }
    }
    notifyLaunchedRunChange();
  }

  /**
   * Invoked to make a best effort to ask the instrumented application to stop
   * collecting data. Note that a successful notification does not mean that
   * data collection has stopped, it may take some time for that to occur.
   * <p>
   * If no launched run exists for the passed run identity string a warning is
   * logged, but otherwise the call has no effect.
   * 
   * @param runIdString
   *          a run identity string.
   * 
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public void requestDataCollectionToStop(@NonNull final String runIdString) {
    if (runIdString == null) {
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    }
    final LaunchedRun lrun = getLaunchedRunFor(runIdString);
    if (lrun == null) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(295, runIdString));
      return;
    }
    final SLJob job = new SLJob() {
      @Override
      public SLStatus run(SLProgressMonitor monitor) {
        monitor.begin(5);
        try {
          if (lrun.getState() == RunState.STOP_COLLECTION_REQUESTED) {
            return SLStatus.OK_STATUS;
          }

          File portFile = new File(getDirectoryFrom(runIdString), InstrumentationConstants.FL_PORT_FILE_LOC);

          monitor.worked(1);
          if (monitor.isCanceled()) {
            return SLStatus.CANCEL_STATUS;
          }

          if (portFile.exists()) {
            try {
              final BufferedReader portReader = new BufferedReader(new FileReader(portFile));
              int port;
              try {
                port = Integer.parseInt(portReader.readLine());
              } finally {
                portReader.close();
              }

              monitor.worked(1);
              if (monitor.isCanceled()) {
                return SLStatus.CANCEL_STATUS;
              }

              final Socket s = new Socket();
              s.connect(new InetSocketAddress("localhost", port));
              try {

                monitor.worked(1);
                if (monitor.isCanceled()) {
                  return SLStatus.CANCEL_STATUS;
                }

                final BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                WatchFlashlightMonitorJob.readUpTo(reader, WatchFlashlightMonitorJob.DELIMITER);
                final PrintWriter writer = new PrintWriter(s.getOutputStream());

                monitor.worked(1);
                if (monitor.isCanceled()) {
                  return SLStatus.CANCEL_STATUS;
                }
                writer.println("stop");
                writer.flush();
              } finally {
                s.close();
              }
              if (lrun.setState(RunState.STOP_COLLECTION_REQUESTED)) {
                notifyLaunchedRunChange();
              }
              return SLStatus.OK_STATUS;
            } catch (IOException e) {
              SLLogger.getLoggerFor(RunManager.class).log(Level.WARNING, e.getMessage(), e);
            }
          }
          SLLogger.getLogger().log(Level.WARNING, I18N.err(296, runIdString, portFile.getAbsolutePath()));
          return SLStatus.OK_STATUS;
        } finally {
          monitor.done();
        }
      }

      @Override
      public String getName() {
        return "Notifying " + runIdString + " to stop Flashlight data collection";
      }
    };
    EclipseUtility.toEclipseJob(job, runIdString).schedule();
  }

  /**
   * Sets the state of if the user wants to see information about a launched
   * run.
   * <p>
   * If no launched run exists for the passed run identity string a warning is
   * logged, but otherwise the call has no effect.
   * 
   * @param runIdString
   *          a run identity string.
   * @param value
   *          {@code true} if the user wants to see this launched run,
   *          {@code false} if the user does not want to see this launched run.
   * 
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public void setDisplayToUser(@NonNull final String runIdString, final boolean value) {
    if (runIdString == null) {
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    }
    boolean notify = false;
    synchronized (f_lock) {
      LaunchedRun lrun = getLaunchedRunFor(runIdString);
      if (lrun == null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(268, runIdString));
      } else {
        notify = lrun.setDisplayToUser(value);
      }
    }
    if (notify) {
      notifyLaunchedRunChange();
    }
  }

  public void setDisplayToUserOnAllFinished(final boolean value) {
    boolean notify = false;
    synchronized (f_lock) {
      for (LaunchedRun lrun : f_launchedRuns) {
        if (RunState.IS_FINISHED.contains(lrun.getState())) {
          notify |= lrun.setDisplayToUser(value);
        }
      }
    }
    if (notify) {
      notifyLaunchedRunChange();
    }
  }

  /*
   * Sets that track run directories where collection has completed. Collection
   * may have completed during this Eclipse session or in a past Eclipse session
   * or even by an Ant task.
   */

  /**
   * Holds the set of all run directories that have completed data collection,
   * but may or may not have been prepared.
   * <p>
   * This set reflects the state of the Flashlight data directory as of the last
   * call to {@code #refresh()}.
   * <p>
   * Invariant: {@link #f_collectionCompletedRunDirectories}
   * <tt>.retainAll(</tt> {@link #f_preparedRunDirectories}<tt>)</tt> contains
   * the same elements as {@link #f_preparedRunDirectories}.
   */
  @UniqueInRegion("RunState")
  private final Set<RunDirectory> f_collectionCompletedRunDirectories = new HashSet<RunDirectory>();

  /**
   * Holds the set of all run directories that have completed data collection
   * and have been prepared. This set is a subset of
   * {@link #f_collectionCompletedRunDirectories}.
   * <p>
   * This set reflects the state of the Flashlight data directory as of the last
   * call to {@link #refresh()}.
   * <p>
   * Invariant: {@link #f_preparedRunDirectories}<tt>.retainAll(</tt>
   * {@link #f_collectionCompletedRunDirectories}<tt>)</tt> contains the same
   * elements as {@link #f_preparedRunDirectories}.
   */
  @UniqueInRegion("RunState")
  private final Set<RunDirectory> f_preparedRunDirectories = new HashSet<RunDirectory>();

  /**
   * Gets the set of all run directories that have completed data collection.
   * The return set can be empty, but will not be {@code null}.
   * <p>
   * The result of this call reflect the state of the Flashlight data directory
   * as of the last call to {@link #refresh()}.
   * 
   * @return the set of run directories that have completed data collection. May
   *         be empty.
   */
  @NonNull
  public Set<RunDirectory> getCollectionCompletedRunDirectories() {
    synchronized (f_lock) {
      return new HashSet<RunDirectory>(f_collectionCompletedRunDirectories);
    }
  }

  /**
   * Gets an array containing the identity strings of all run directories
   * managed by this. The identity string for a {@link RunDirectory}, which we
   * will call <tt>run</tt>, is defined to be
   * {@code run.getDescription().getRunIdString()}.
   * <p>
   * These strings are useful as the access keys for a job that wants to scan
   * all the Flashlight data directories.
   * <p>
   * The result of this call reflects the state of the Flashlight data directory
   * as of the last call to {@link #refresh()}.
   * 
   * @return the identity strings of all run directories that have completed
   *         data collection. May be empty.
   */
  @NonNull
  public String[] getCollectionCompletedRunIdStrings() {
    final Set<RunDirectory> runs = getCollectionCompletedRunDirectories();
    if (runs.isEmpty()) {
      return SLUtility.EMPTY_STRING_ARRAY;
    }
    final String[] ids = new String[runs.size()];
    int index = 0;
    for (final RunDirectory run : runs) {
      ids[index] = run.getRunIdString();
      index++;
    }
    return ids;
  }

  /**
   * Looks up a run directory that has completed data collection by the passed
   * identity string. In particular for the returned {@link RunDirectory}, which
   * we will call <tt>run</tt>,
   * {@code runDirectory.getRunIdString().equals(runIdentityString)} is
   * {@code true}.
   * <p>
   * The result of this call reflects the state of the Flashlight data directory
   * as of the last call to {@link #refresh()}.
   * 
   * @param runIdString
   *          a run identity string.
   * @return run directory managed by this with the passed identity string, or
   *         {@code null} if none.
   */
  @Nullable
  public RunDirectory getCollectionCompletedRunDirectoryByIdString(final String runIdString) {
    for (final RunDirectory runDirectory : getCollectionCompletedRunDirectories()) {
      if (runDirectory.getRunIdString().equals(runIdString)) {
        return runDirectory;
      }
    }
    return null;
  }

  /**
   * Gets the set of run directories that have completed data collection and
   * been prepared for querying. These run directories are ready to be queries.
   * The return set can be empty, but will not be {@code null}.
   * <p>
   * The result of this call reflects the state of the Flashlight data directory
   * as of the last call to {@link #refresh()}.
   * 
   * @return the set of run directories managed by this that have been prepared.
   *         May be empty.
   */
  @NonNull
  public Set<RunDirectory> getPreparedRunDirectories() {
    synchronized (f_lock) {
      return new HashSet<RunDirectory>(f_preparedRunDirectories);
    }
  }

  /**
   * Gets the set of run directories that have completed data collection but
   * have not yet been prepared or are not yet currently being prepared. The
   * return set can be empty, but will not be {@code null}.
   * <p>
   * The set of run directories considered by this call reflects the state of
   * the Flashlight data directory as of the last call to {@link #refresh()}.
   * However, the check if the run directory is prepared or has an active job
   * preparing it is reflects the situation as the call is made.
   * 
   * @return the set of run directories managed by this that have not been
   *         prepared. May be empty.
   */
  @NonNull
  public Set<RunDirectory> getCollectionCompletedRunDirectoriesNotPreparedOrBeingPrepared() {
    final Set<RunDirectory> result = new HashSet<RunDirectory>();
    synchronized (f_lock) {
      for (RunDirectory runDir : f_collectionCompletedRunDirectories) {
        if (!runDir.isPrepared() && !isBeingPrepared(runDir)) {
          result.add(runDir);
        }
      }
    }
    return result;
  }

  /**
   * Checks if the passed run is in the process of being prepared for querying.
   * <p>
   * The check that there is an active data preparation job reflects the
   * situation as the call is made.
   * 
   * @param directory
   *          a Flashlight run directory.
   * @return {@code true} if the passed run is in the process of being prepared
   *         for querying, {@code false} otherwise.
   */
  public boolean isBeingPrepared(final RunDirectory directory) {
    return findPrepSLJobOrNullFor(directory) != null;
  }

  /**
   * Finds a running data preparation job for the passed run directory and
   * returns it, {@code null} is returned if no data preparation job is
   * currently running on the passed directory.
   * <p>
   * The search for an active data preparation job reflects the situation as the
   * call is made.
   * 
   * @param directory
   *          a Flashlight run directory.
   * @return the running data preparation job for the passed run directory or
   *         {@code null} if no data preparation job is currently running on the
   *         passed directory
   */
  @Nullable
  public PrepSLJob findPrepSLJobOrNullFor(final RunDirectory directory) {
    if (directory.getPrepDbDirectoryHandle().exists() && !directory.isPrepared()) {
      final String prepJobName = directory.getPrepJobName();
      final List<PrepSLJob> prepJobs = EclipseUtility.getActiveJobsOfType(PrepSLJob.class);
      for (PrepSLJob job : prepJobs) {
        if (prepJobName.equals(job.getName())) {
          return job;
        }
      }
    }
    return null;
  }

  /**
   * Starts a data preparation job on the passed run directory. This call does
   * not block, it returns immediately after a job is submitted to Eclipse.
   * 
   * @param run
   *          a run directory.
   * 
   * @throws IllegalArgumentException
   *           if run is {@code null}.
   */
  private void prepare(@NonNull final RunDirectory run) {
    if (run == null) {
      throw new IllegalArgumentException(I18N.err(44, "run"));
    }

    final SLJob job = new PrepSLJob(run, EclipseUtility.getIntPreference(FlashlightPreferencesUtility.PREP_OBJECT_WINDOW_SIZE),
        AdHocDataSource.getManager().getTopLevelQueries());
    final Job eJob = EclipseUtility.toEclipseJob(job, run.getRunIdString());
    eJob.schedule();
    notifyPrepareDataJobScheduled();
  }

  /**
   * Starts a data preparation job on all passed run directories. This call does
   * not block, it returns immediately after any necessary jobs are submitted to
   * Eclipse.
   * 
   * @param runs
   *          a collection of run directories.
   * 
   * @throws IllegalArgumentException
   *           if runs is {@code null}.
   */
  public void prepareAll(@NonNull List<RunDirectory> runs) {
    if (runs == null) {
      throw new IllegalArgumentException(I18N.err(44, "runs"));
    }

    for (RunDirectory run : runs)
      prepare(run);

    refresh(true);
  }

  @NonNull
  private static Set<RunDescription> getRunDescriptionsFor(Set<RunDirectory> runs) {
    final Set<RunDescription> result = new HashSet<RunDescription>(runs.size());
    for (RunDirectory runDir : runs) {
      result.add(runDir.getDescription());
    }
    return result;
  }

  /**
   * Submits a job to refresh this manager by scanning the contents of the
   * Flashlight data directory.
   * <p>
   * This call does not block, it just schedules the job and returns.
   * 
   * @param afterRunDirJobs
   *          {@code true} if the job should be blocked until all other run
   *          directory jobs are completed, {@code false} otherwise.
   */
  public void refresh(final boolean afterRunDirJobs) {
    final Set<String> keys = new HashSet<String>();
    keys.add(RunManager.class.getName()); // used to serialize refreshes
    if (afterRunDirJobs) {
      for (RunDirectory rd : getCollectionCompletedRunDirectories())
        keys.add(rd.getRunIdString());
    }
    final String[] accessKeys = keys.toArray(new String[keys.size()]);
    EclipseUtility.toEclipseJob(f_refreshJob, accessKeys).schedule(10);

  }

  @Vouch("ThreadSafe")
  private final TickListener f_tick = new TickListener() {
    public void timingSourceTick(TimingSource source, long nanoTime) {
      refresh(false);
    }
  };

  @Vouch("ThreadSafe")
  private final SLJob f_refreshJob = new AbstractSLJob("Examining the Flashlight run directory contents") {

    @Override
    public SLStatus run(SLProgressMonitor monitor) {
      monitor.begin(5);
      try {
        /*
         * Assume nothing changed
         */
        boolean collectionCompletedRunDirectoryChange = false;
        boolean preparedDataChanged = false;
        boolean launchedRunChange = false;

        /*
         * Search the run directory for runs that have completed data
         * collection.
         */
        final Collection<RunDirectory> collectionCompletedDirs = FlashlightFileUtility.getRunDirectories(f_dataDir);

        monitor.worked(1);
        if (monitor.isCanceled())
          return SLStatus.CANCEL_STATUS;

        final Set<RunDescription> collectionCompleted = new HashSet<RunDescription>();
        final Collection<RunDirectory> preparedDirs = new ArrayList<RunDirectory>();
        final Set<RunDescription> prepared = new HashSet<RunDescription>();
        for (final RunDirectory dir : collectionCompletedDirs) {
          final RunDescription desc = dir.getDescription();
          collectionCompleted.add(desc);
          if (dir.isPrepared()) {
            preparedDirs.add(dir);
            prepared.add(desc);
          }
        }

        monitor.worked(1);
        if (monitor.isCanceled())
          return SLStatus.CANCEL_STATUS;

        /*
         * Check if anything changed.
         * 
         * If so, update the fields and notify observers.
         */
        synchronized (f_lock) {
          if (!getRunDescriptionsFor(f_collectionCompletedRunDirectories).equals(collectionCompleted)) {
            collectionCompletedRunDirectoryChange = true;
          }
          if (!getRunDescriptionsFor(f_preparedRunDirectories).equals(prepared)) {
            collectionCompletedRunDirectoryChange = true;
            preparedDataChanged = true;
          }

          monitor.worked(1);
          if (monitor.isCanceled())
            return SLStatus.CANCEL_STATUS;

          if (collectionCompletedRunDirectoryChange) {
            f_collectionCompletedRunDirectories.clear();
            f_collectionCompletedRunDirectories.addAll(collectionCompletedDirs);
            f_preparedRunDirectories.clear();
            f_preparedRunDirectories.addAll(preparedDirs);

            monitor.worked(1);

            for (RunDirectory run : collectionCompletedDirs) {
              final LaunchedRun lrun = getLaunchedRunFor(run.getRunIdString());
              if (lrun != null) {
                final boolean isPrepared = f_preparedRunDirectories.contains(run);
                if (isPrepared) {
                  if (lrun.setState(RunState.READY)) {
                    launchedRunChange = true;
                  }
                } else {
                  if (lrun.setState(RunState.DONE_COLLECTING_DATA)) {
                    launchedRunChange = true;
                    if (EclipseUtility.getBooleanPreference(FlashlightPreferencesUtility.AUTO_PREP_LAUNCHED_RUNS))
                      prepare(run);
                  }
                }
              }
            }
          }
        }
        monitor.worked(1);
        /*
         * We must be careful to not notify holding a lock.
         */
        if (preparedDataChanged) {
          /*
           * Prepared data changed -- check if we are in the Flashlight
           * perspective
           */
          (new SwitchToFlashlightPerspectiveJob()).schedule(500);
        }
        if (collectionCompletedRunDirectoryChange) {
          notifyCollectionCompletedRunDirectoryChange();
        }
        if (launchedRunChange) {
          notifyLaunchedRunChange();
        }
        return SLStatus.OK_STATUS;
      } catch (final Exception e) {
        return SLStatus.createErrorStatus(SLStatus.OK, "RunManager.refresh() failed", e);
      } finally {
        monitor.done();
      }
    }
  };
}
