package com.surelogic.flashlight.client.eclipse.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.Singleton;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.client.eclipse.jobs.RefreshRunManagerSLJob;
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
public final class RunManager {

  private static final RunManager INSTANCE = new RunManager();

  /**
   * Gets the singleton instance of the Flashlight run manager.
   * 
   * @return the singleton instance of the Flashlight run manager.
   */
  @NonNull
  public static RunManager getInstance() {
    return INSTANCE;
  }

  @Unique("return")
  @Vouch("No need to check utility call")
  private RunManager() {
    f_dataDir = EclipseUtility.getFlashlightDataDirectory();
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
  private void notifyInstrumentedApplicationChange() {
    for (final IRunManagerObserver o : f_observers) {
      o.notifyInstrumentedApplicationChange();
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

  private final Object f_lock = new Object();

  @UniqueInRegion("RunState")
  private final Set<String> f_launchingRunIdStrings = new HashSet<String>();

  @UniqueInRegion("RunState")
  private final Set<String> f_collectingRunIdStrings = new HashSet<String>();

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
    if (directory == null)
      throw new IllegalArgumentException(I18N.err(44, "directory"));
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
    if (runIdString == null)
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    final File result = new File(getDirectory(), runIdString);
    return result;
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
    if (runIdString == null)
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    synchronized (f_lock) {
      if (f_launchingRunIdStrings.contains(runIdString)) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(168, runIdString));
        return;
      }
      f_launchingRunIdStrings.add(runIdString);
      notifyInstrumentedApplicationChange();
    }
  }

  /**
   * Notifies the run manager that an application is collecting data.
   * 
   * @param runIdString
   *          a run identity string.
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public void notifyCollectingData(@NonNull final String runIdString) {
    if (runIdString == null)
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    synchronized (f_lock) {
      if (f_collectingRunIdStrings.contains(runIdString)) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(170, runIdString));
        return;
      }
      if (f_launchingRunIdStrings.contains(runIdString)) {
        f_launchingRunIdStrings.remove(runIdString);
      } else {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(235, runIdString));
      }
      f_collectingRunIdStrings.add(runIdString);
      notifyInstrumentedApplicationChange();
    }
  }

  /**
   * Invoked to make a best effort to ask the instrumented application to stop
   * collecting data. Note that a successful notification does not mean that
   * data collection has stopped, it may take some time for that to occur.
   * 
   * @param runIdString
   *          a run identity string.
   * @return {@code true} if the instrumented application was notified
   *         successfully, {@code false} if something went wrong and no
   *         notification could be performed.
   * @throws IllegalArgumentException
   *           if runIdString is {@code null}.
   */
  public boolean requestDataCollectionToStop(@NonNull final String runIdString) {
    if (runIdString == null)
      throw new IllegalArgumentException(I18N.err(44, "runIdString"));
    // TODO
    return false;
  }

  /**
   * Holds the set of all run directories that have completed data collection,
   * but may or may not have been prepared.
   * <p>
   * This set reflects the state of the Flashlight data directory as of the last
   * call to {@code #refresh()}.
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
    if (runs.isEmpty())
      return SLUtility.EMPTY_STRING_ARRAY;
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
        if (!runDir.isPrepared() && !isBeingPrepared(runDir))
          result.add(runDir);
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
   * not block, it returns immediately after the job is submitted to Eclipse.
   * 
   * @param run
   *          a run directory.
   */
  public void startDataPreparationJobOn(@NonNull final RunDirectory run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));

    final SLJob job = new PrepSLJob(run, EclipseUtility.getIntPreference(FlashlightPreferencesUtility.PREP_OBJECT_WINDOW_SIZE),
        AdHocDataSource.getManager().getTopLevelQueries());
    final Job eJob = EclipseUtility.toEclipseJob(job, run.getRunIdString());
    eJob.setUser(true);
    eJob.schedule();
    notifyPrepareDataJobScheduled();
  }

  /**
   * Refreshes the set of run descriptions managed by this class and notifies
   * all observers if that set has changed. This method is invoked by
   * {@link RefreshRunManagerSLJob}, which is generally what you want to use if
   * you are in the Eclipse client.
   */
  public void refresh() {
    /*
     * Assume nothing changed
     */
    boolean isChanged = false;

    /*
     * Search the run directory for runs that have completed data collection.
     */
    final Collection<RunDirectory> collectionCompletedDirs = FlashlightFileUtility.getRunDirectories(f_dataDir);

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

    /*
     * Check if anything changed.
     * 
     * If so, update the fields and notify observers.
     */
    synchronized (f_lock) {
      if (!getRunDescriptionsFor(f_collectionCompletedRunDirectories).equals(collectionCompleted)) {
        isChanged = true;
      }
      if (!getRunDescriptionsFor(f_preparedRunDirectories).equals(prepared)) {
        isChanged = true;
      }
      if (isChanged) {
        f_collectionCompletedRunDirectories.clear();
        f_collectionCompletedRunDirectories.addAll(collectionCompletedDirs);
        f_preparedRunDirectories.clear();
        f_preparedRunDirectories.addAll(preparedDirs);
      }
    }

    /*
     * We must be careful to not notify holding a lock.
     */
    if (isChanged) {
      notifyCollectionCompletedRunDirectoryChange();
    }
  }

  @NonNull
  private static Set<RunDescription> getRunDescriptionsFor(Set<RunDirectory> runs) {
    final Set<RunDescription> result = new HashSet<RunDescription>(runs.size());
    for (RunDirectory runDir : runs) {
      result.add(runDir.getDescription());
    }
    return result;
  }
}
