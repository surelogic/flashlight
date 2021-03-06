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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;
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
import com.surelogic.common.CommonImages;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLJobTracker;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.SLImages;
import com.surelogic.flashlight.client.eclipse.dialogs.RunControlDialog;
import com.surelogic.flashlight.client.eclipse.jobs.SwitchToFlashlightPerspectiveJob;
import com.surelogic.flashlight.client.eclipse.jobs.WatchFlashlightMonitorJob;
import com.surelogic.flashlight.client.eclipse.preferences.FlashlightPreferencesUtility;
import com.surelogic.flashlight.client.eclipse.views.adhoc.FlashlightDataSource;
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

  private final TimingSource f_timer = new ScheduledExecutorTimingSource(6, TimeUnit.SECONDS);

  /**
   * Automatically called by {@link #getInstance()} &mdash; client code should
   * <b>never</b> invoke.
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
  private final CopyOnWriteArraySet<IRunManagerObserver> f_observers = new CopyOnWriteArraySet<>();

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
  void notifyCollectionCompletedRunDirectoryChange() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyCollectionCompletedRunDirectoryChange();
    }
  }

  /**
   * Do not call this method while holding a lock!
   */
  void notifyLaunchedRunChange() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyLaunchedRunChange();
    }
  }

  /**
   * Do not call this method while holding a lock!
   */
  void notifyPrepareDataJobScheduled() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyPrepareDataJobScheduled();
    }
  }

  /**
   * A reference to the Flashlight data directory.
   */
  @Vouch("ThreadSafe")
  final File f_dataDir;

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
  final Object f_lock = new Object();

  /*
   * Sets that track launching, collection, and termination of an instrumented
   * program.
   * 
   * These use the run identity string because, except for the last set,
   * collection has not yet completed on these runs.
   */

  @UniqueInRegion("RunState")
  final LinkedList<LaunchedRun> f_launchedRuns = new LinkedList<>();

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
      return new ArrayList<>(f_launchedRuns);
    }
  }

  /**
   * Gets if the passed run identity string for a launched run is finished
   * collecting data. This method checks both the state of the
   * {@link LaunchedRun}, or, if none, the state of the Flashlight data
   * directory on the disk.
   * 
   * @param runIdString
   *          a run identity string.
   * @return {@code true} if the passed run identity string for a launched run
   *         is finished collecting data, {@code false} otherwise.
   */
  public boolean isLaunchedRunFinishedCollectingData(@NonNull final String runIdString) {
    if (runIdString == null)
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    synchronized (f_lock) {
      final LaunchedRun lrun = getLaunchedRunFor(runIdString);
      if (lrun != null)
        return RunState.IS_FINISHED_COLLECTING_DATA.contains(lrun.getState());
      else {
        return getCollectionCompletedRunDirectories().contains(getDirectoryFrom(runIdString));
      }
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
  LaunchedRun getLaunchedRunFor(@NonNull final String runIdString) {
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
   * launched. This is the first method called by launch code and it outputs a
   * warning if it is called twice with the same {@code runIdString}.
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
   * Notifies the run manager that a launch in the process of performing
   * instrumentation and launch of a program or application was cancelled or
   * failed in some way prior to invoking {@link #notifyCollectingData(String)}.
   * <p>
   * This situation can occur when an Android app is launched in the IDE and no
   * device or emulator exists that can run it, so the user cancels the launch.
   * 
   * @param runIdString
   *          a run identity string.
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public void notifyLaunchCancelledPriorToCollectingData(@NonNull final String runIdString) {
    if (runIdString == null) {
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    }
    synchronized (f_lock) {
      LaunchedRun lrun = getLaunchedRunFor(runIdString);
      if (lrun == null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(235, runIdString));
        return;
      }
      final RunState old = lrun.setStateAndReturnOld(RunState.LAUNCH_CANCELLED);
      if (old != RunState.INSTRUMENTATION_AND_LAUNCH) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(236, runIdString));
        return;
      }
    }
    notifyLaunchedRunChange();
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
    final SLJob job = new AbstractSLJob("Notifying " + runIdString + " to stop Flashlight data collection") {
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

  /**
   * Sets the state of if the user wants to see information about all launched
   * runs in the {@link RunState#READY} state.
   * 
   * @param value
   *          {@code true} if the user wants to see this launched run,
   *          {@code false} if the user does not want to see this launched run.
   */
  public void setDisplayToUserIfReadyOrCancelled(final boolean value) {
    boolean notify = false;
    synchronized (f_lock) {
      for (LaunchedRun lrun : f_launchedRuns) {
        if (RunState.READY.equals(lrun.getState()) || RunState.LAUNCH_CANCELLED.equals(lrun.getState())) {
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
  final Set<RunDirectory> f_collectionCompletedRunDirectories = new HashSet<>();

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
  final Set<RunDirectory> f_preparedRunDirectories = new HashSet<>();

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
      return new HashSet<>(f_collectionCompletedRunDirectories);
    }
  }

  /**
   * This method returns the run directories that have completed data collection
   * in most recent to least recent order as an array. The result is intended as
   * a good starting point for the user interface.
   * 
   * @return a list of run directories that have completed data collection
   *         sorted for the user interface. May be empty.
   */
  @NonNull
  public RunDirectory[] getCollectionCompletedRunDirectoriesForUI() {
    final ArrayList<RunDirectory> result = new ArrayList<>(getCollectionCompletedRunDirectories());
    Collections.sort(result, new Comparator<RunDirectory>() {
      public int compare(RunDirectory o1, RunDirectory o2) {
        if (o1 == null || o2 == null)
          return 0;
        String s1 = SLUtility.toStringDayHMS(o1.getDescription().getStartTimeOfRun());
        String s2 = SLUtility.toStringDayHMS(o2.getDescription().getStartTimeOfRun());
        return s2.compareTo(s1);
      }
    });
    return result.toArray(new RunDirectory[result.size()]);
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
      return new HashSet<>(f_preparedRunDirectories);
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
    final Set<RunDirectory> result = new HashSet<>();
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
   * Starts a data preparation job on all passed run directories. This call does
   * not block, it returns immediately after any necessary jobs are submitted to
   * Eclipse.
   * <p>
   * Observers are notified after all jobs are scheduled. Refresh jobs for the
   * Flashlight data directory are also scheduled.
   * 
   * @param runs
   *          a collection of run directories.
   * 
   * @throws IllegalArgumentException
   *           if runs is {@code null}.
   */
  public void prepareAll(@NonNull Collection<RunDirectory> runs) {
    if (runs == null) {
      throw new IllegalArgumentException(I18N.err(44, "runs"));
    }

    synchronized (f_lock) {
      for (final RunDirectory run : runs) {
        if (run == null)
          throw new IllegalArgumentException(I18N.err(44, "run"));

        final SLJob job = new PrepSLJob(run, EclipseUtility.getIntPreference(FlashlightPreferencesUtility.PREP_OBJECT_WINDOW_SIZE),
            FlashlightDataSource.getManager().getTopLevelQueries());
        final Job eJob = EclipseUtility.toEclipseJob(job, run.getRunIdString());
        eJob.setProperty(IProgressConstants.ICON_PROPERTY, SLImages.getImageDescriptor(CommonImages.IMG_FL_PREP_DATA));
        final LaunchedRun lrun = getLaunchedRunFor(run.getRunIdString());
        if (lrun != null) {
          final SLJobTracker tracker = new SLJobTracker(job);
          lrun.setPrepareJobTracker(tracker);
        }
        eJob.schedule();
      }
    }
    notifyPrepareDataJobScheduled();
    refresh();
  }

  /**
   * Starts a data preparation job on the passed run directory. This call does
   * not block, it returns immediately after any necessary jobs are submitted to
   * Eclipse.
   * <p>
   * Observers are notified after the job is scheduled. Refresh jobs for the
   * Flashlight data directory are also scheduled.
   * 
   * @param run
   *          a run directory.
   * 
   * @throws IllegalArgumentException
   *           if run is {@code null}.
   */
  public void prepare(@NonNull final RunDirectory run) {
    if (run == null) {
      throw new IllegalArgumentException(I18N.err(44, "run"));
    }

    List<RunDirectory> runs = new ArrayList<>();
    runs.add(run);
    prepareAll(runs);
  }

  @NonNull
  static Set<RunDescription> getRunDescriptionsFor(Set<RunDirectory> runs) {
    final Set<RunDescription> result = new HashSet<>(runs.size());
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
   */
  public void refresh() {
    EclipseUtility.toEclipseJob(f_refreshJob, getRefreshAccessKey()).schedule(100);
  }

  /**
   * Gets the job access key used to serialize jobs started by
   * {@link #refresh()}. This key is needed if jobs, such as a delete, don't
   * want a refresh running at the same time.
   * 
   * @return the job access key used to serialize jobs started by
   *         {@link #refresh()}.
   */
  public String getRefreshAccessKey() {
    return RunManager.class.getName();
  }

  @Vouch("ThreadSafe")
  private final TickListener f_tick = new TickListener() {
    @Override
    public void timingSourceTick(TimingSource source, long nanoTime) {
      refresh();
    }
  };

  @Vouch("ThreadSafe")
  private final SLJob f_refreshJob = new AbstractSLJob("Examining the Flashlight run directory contents") {

    private final AtomicBoolean f_firstRefresh = new AtomicBoolean(true);

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
        List<RunDirectory> prepare = new ArrayList<>();

        /*
         * Search the run directory for runs that have completed data
         * collection.
         */
        final Collection<RunDirectory> collectionCompletedDirs = FlashlightFileUtility.getRunDirectories(f_dataDir);

        monitor.worked(1);
        if (monitor.isCanceled())
          return SLStatus.CANCEL_STATUS;

        final Set<RunDescription> collectionCompleted = new HashSet<>();
        final Collection<RunDirectory> preparedDirs = new ArrayList<>();
        final Set<RunDescription> prepared = new HashSet<>();
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

            /*
             * Based upon what we read from the disk see if we need to update
             * the status of a launched run. It could be done collecting or done
             * being prepared.
             */
            for (final RunDirectory run : collectionCompletedDirs) {
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
                    if (EclipseUtility.getBooleanPreference(FlashlightPreferencesUtility.AUTO_PREP_LAUNCHED_RUNS)) {
                      prepare.add(run);
                    }
                  }
                }
              }
            }
          }

          /*
           * Check if a prep job currently being monitored by a launched run has
           * finished. We also clear out ready runs that are no longer being
           * displayed or they got deleted.
           */
          for (Iterator<LaunchedRun> iterator = f_launchedRuns.iterator(); iterator.hasNext();) {
            final LaunchedRun lrun = iterator.next();

            final SLJobTracker tracker = lrun.getPrepareJobTracker();
            if (tracker != null) {
              if (tracker.isFinished()) {
                tracker.clearObservers();
                lrun.setPrepareJobTracker(null);
                launchedRunChange = true;
              }
            }

            // clear out launches that are not displayed and ready
            if (!lrun.getDisplayToUser() && lrun.isReady()) {
              iterator.remove();
            }
            // clear out launches that have been deleted
            if (!RunState.INSTRUMENTATION_AND_LAUNCH.equals(lrun.getState())
                && !getDirectoryFrom(lrun.getRunIdString()).isDirectory()) {
              iterator.remove();
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
          if (!f_firstRefresh.get())
            (new SwitchToFlashlightPerspectiveJob()).schedule(500);
        }
        if (collectionCompletedRunDirectoryChange) {
          notifyCollectionCompletedRunDirectoryChange();
        }
        if (launchedRunChange) {
          notifyLaunchedRunChange();
        }
        if (!prepare.isEmpty()) {
          prepareAll(prepare);
        }
        return SLStatus.OK_STATUS;
      } catch (final Exception e) {
        return SLStatus.createErrorStatus(SLStatus.OK, "RunManager.refresh() failed", e);
      } finally {
        monitor.done();
        f_firstRefresh.set(false);
      }
    }
  };
}
