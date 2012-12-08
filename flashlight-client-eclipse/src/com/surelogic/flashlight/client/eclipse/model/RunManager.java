package com.surelogic.flashlight.client.eclipse.model;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Singleton;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.flashlight.client.eclipse.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.jobs.PrepSLJob;
import com.surelogic.flashlight.common.model.FlashlightFileUtility;
import com.surelogic.flashlight.common.model.RunDescription;
import com.surelogic.flashlight.common.model.RunDirectory;

/**
 * A singleton that manages the set of run directory aggregates.
 */
@ThreadSafe
@Singleton
@Region("private RunDirectories")
@RegionLock("RunDirectoriesLock is f_runs protects RunDirectories")
public final class RunManager {

  private static final RunManager INSTANCE = new RunManager();

  /**
   * Gets the singleton instance.
   * 
   * @return the singleton instance.
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
  private void notifyObservers() {
    for (final IRunManagerObserver o : f_observers) {
      o.notify(this);
    }
  }

  /**
   * A reference to the Flashlight data directory.
   */
  @Vouch("ThreadSafe")
  private final File f_dataDir;

  /**
   * Gets the location of the Flashlight data directory.
   * 
   * @return the Flashlight data directory.
   */
  @NonNull
  public File getDataDirectory() {
    return f_dataDir;
  }

  /**
   * Holds the set of all known run directories.
   */
  @UniqueInRegion("RunDirectories")
  private final Set<RunDirectory> f_runs = new HashSet<RunDirectory>();

  /**
   * Gets the set of all run directories managed by this. The return set can be
   * empty, but will not be {@code null}.
   * 
   * @return the set of run directories managed by this.
   */
  @NonNull
  public Set<RunDirectory> getCollectionCompletedRunDirectories() {
    synchronized (f_runs) {
      return new HashSet<RunDirectory>(f_runs);
    }
  }

  /**
   * Gets an array containing the identity strings of all run directories
   * managed by this. The identity string for a {@link RunDirectory}, which we
   * will call <tt>run</tt>, is defined to be
   * {@code run.getDescription().getRunIdString()}.
   * 
   * @return the identity strings of all run directories managed by this.
   */
  @NonNull
  public String[] getRunIdentities() {
    final Set<RunDirectory> runs = getCollectionCompletedRunDirectories();
    final Set<String> ids = new HashSet<String>(runs.size());
    for (final RunDirectory run : runs) {
      ids.add(run.getRunIdString());
    }
    return ids.toArray(SLUtility.EMPTY_STRING_ARRAY);
  }

  /**
   * Looks up a run directory managed by this with the passed identity string.
   * In particular for the returned {@link RunDirectory}, which we will call
   * <tt>run</tt>,
   * {@code runDirectory.getRunIdString().equals(runIdentityString)} is
   * {@code true}.
   * 
   * @param runIdentityString
   *          a run identity string.
   * @return run directory managed by this with the passed identity string, or
   *         {@code null} if none.
   */
  @Nullable
  public RunDirectory getRunDirectoryByIdentityString(final String runIdentityString) {
    for (final RunDirectory runDirectory : getCollectionCompletedRunDirectories()) {
      if (runDirectory.getRunIdString().equals(runIdentityString)) {
        return runDirectory;
      }
    }
    return null;
  }

  /**
   * Gets the set of prepared run directories managed by this. These run
   * directories are ready to be queries. The return set can be empty, but will
   * not be {@code null}.
   * 
   * @return the set of run directories managed by this that have been prepared.
   *         May be empty.
   */
  @NonNull
  public Set<RunDirectory> getPreparedRunDirectories() {
    final Set<RunDirectory> result = new HashSet<RunDirectory>();
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        if (runDir.isPrepared())
          result.add(runDir);
      }
    }
    return result;
  }

  /**
   * Gets the set of run directories managed by this that have not been
   * prepared. The return set can be empty, but will not be {@code null}.
   * 
   * @return the set of run directories managed by this that have not been
   *         prepared. May be empty.
   */
  @NonNull
  public Set<RunDirectory> getNotPreparedOrBeingPreparedRunDirectories() {
    final Set<RunDirectory> result = new HashSet<RunDirectory>();
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        if (!runDir.isPrepared() && !isBeingPrepared(runDir))
          result.add(runDir);
      }
    }
    return result;
  }

  /**
   * Checks if the passed run is in the process of being prepared for querying.
   * <p>
   * This cannot be a call on the {@link RunDirectory} because we need to check
   * if a prep job is currently running within Eclipse and that class cannot
   * interact with Eclipse.
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
   * Finds the running data preparation job for the passed run directory and
   * returns it, or {@code null} if no data preparation job is currently running
   * on the passed directory.
   * 
   * @param directory
   *          a Flashlight run directory.
   * @return the running data preparation job for the passed run directory and
   *         returns it, or {@code null} if no data preparation job is currently
   *         running on the passed directory
   */
  @Nullable
  public PrepSLJob findPrepSLJobOrNullFor(final RunDirectory directory) {
    if (directory.getPrepDbDirectoryHandle().exists() && !directory.isPrepared()) {
      final String prepJobName = directory.getPrepJobName();
      final List<PrepSLJob> prepJobs = EclipseJob.getInstance().getActiveJobsOfType(PrepSLJob.class);
      for (PrepSLJob job : prepJobs) {
        if (prepJobName.equals(job.getName())) {
          return job;
        }
      }
    }
    return null;
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
     * Examine the run directory
     */
    final Set<RunDescription> runs = new HashSet<RunDescription>();
    final Set<RunDescription> preparedRuns = new HashSet<RunDescription>();
    final Collection<RunDirectory> runDirs = FlashlightFileUtility.getRunDirectories(f_dataDir);
    for (final RunDirectory dir : runDirs) {
      runs.add(dir.getDescription());
      if (dir.isPrepared()) {
        preparedRuns.add(dir.getDescription());
      }
    }

    /*
     * Check if anything changed...if so, update the fields and notify
     * observers.
     */
    synchronized (f_runs) {
      if (!getRunDescriptions().equals(runs)) {
        isChanged = true;
      }
      if (!getPreparedRunDescriptions().equals(preparedRuns)) {
        isChanged = true;
      }
      if (isChanged) {
        f_runs.clear();
        f_runs.addAll(runDirs);
      }
    }

    if (isChanged) {
      // System.out.println("RunManager.notifyObservers() invoked");
      notifyObservers();
    }
  }

  @NonNull
  @RequiresLock("RunDirectoriesLock")
  private Set<RunDescription> getRunDescriptions() {
    final Set<RunDescription> result = new HashSet<RunDescription>(f_runs.size());
    for (RunDirectory runDir : f_runs) {
      result.add(runDir.getDescription());
    }
    return result;
  }

  @NonNull
  @RequiresLock("RunDirectoriesLock")
  private Set<RunDescription> getPreparedRunDescriptions() {
    final Set<RunDescription> result = new HashSet<RunDescription>();
    for (RunDirectory runDir : f_runs) {
      if (runDir.isPrepared())
        result.add(runDir.getDescription());
    }
    return result;
  }
}
