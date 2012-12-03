package com.surelogic.flashlight.client.eclipse.model;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
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
import com.surelogic.flashlight.client.eclipse.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.model.RunDescription;

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
   * 
   * @see #refresh(boolean)
   */
  @NonNull
  public Set<RunDirectory> getRunDirectories() {
    synchronized (f_runs) {
      return new HashSet<RunDirectory>(f_runs);
    }
  }

  /**
   * Gets an array containing the identity strings of all run directories
   * managed by this. The identity string for a {@link RunDirectory}, which we
   * will call <tt>run</tt>, is defined to be
   * {@code run.getDescription().toIdentityString()}.
   * 
   * @return the identity strings of all run directories managed by this.
   */
  @NonNull
  public String[] getRunIdentities() {
    final Set<RunDirectory> runs = getRunDirectories();
    final Set<String> ids = new HashSet<String>(runs.size());
    for (final RunDirectory run : runs) {
      ids.add(run.getDescription().toIdentityString());
    }
    return ids.toArray(SLUtility.EMPTY_STRING_ARRAY);
  }

  /**
   * Looks up a run directory managed by this with the passed identity string.
   * In particular for the returned {@link RunDirectory}, which we will call
   * <tt>run</tt>,
   * {@code run.getDescription().toIdentityString.equals(runIdentityString)} is
   * {@code true}.
   * 
   * @param runIdentityString
   *          a run identity string.
   * @return run directory managed by this with the passed identity string, or
   *         {@code null} if none.
   */
  @Nullable
  public RunDirectory getRunDirectoryByIdentityString(final String runIdentityString) {
    for (final RunDirectory runDirectory : getRunDirectories()) {
      if (runDirectory.getDescription().toIdentityString().equals(runIdentityString)) {
        return runDirectory;
      }
    }
    return null;
  }

  /**
   * Gets the set of prepared run directories managed by this. The return set
   * can be empty, but will not be {@code null}.
   * 
   * @return the set of run directories managed by this that have been prepared.
   *         May be empty.
   */
  @NonNull
  public Set<RunDirectory> getPreparedRunDirectories() {
    final Set<RunDirectory> result = new HashSet<RunDirectory>();
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        if (runDir.isPreparedOrIsBeingPrepared())
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
  public Set<RunDirectory> getNotPreparedRunDirectories() {
    final Set<RunDirectory> result = new HashSet<RunDirectory>();
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        if (!runDir.isPreparedOrIsBeingPrepared())
          result.add(runDir);
      }
    }
    return result;
  }

  /**
   * Refreshes the set of run descriptions managed by this class and notifies
   * all observers if that set has changed. This method is invoked by
   * {@link RefreshRunManagerSLJob}, which is generally what you want to use if
   * you are in the Eclipse client.
   * 
   * @param forceNotify
   *          {@code true} if a notification to observers is made even if no
   *          changes are noted, {@code false} if a notification to observers is
   *          only made if changes are noted.
   * 
   * @see RefreshRunManagerSLJob
   */
  public void refresh(boolean forceNotify) {
    /*
     * Assume nothing changed
     */
    boolean isChanged = false;

    /*
     * Examine the run directory
     */
    final Set<RunDescription> runs = new HashSet<RunDescription>();
    final Set<RunDescription> preparedRuns = new HashSet<RunDescription>();
    final Collection<RunDirectory> runDirs = RawFileUtility.getRunDirectories(f_dataDir);
    for (final RunDirectory dir : runDirs) {
      runs.add(dir.getDescription());
      if (dir.isPreparedOrIsBeingPrepared()) {
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

    if (isChanged || forceNotify) {
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
      if (runDir.isPreparedOrIsBeingPrepared())
        result.add(runDir.getDescription());
    }
    return result;
  }
}
