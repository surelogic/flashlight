package com.surelogic.flashlight.client.eclipse.model;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Singleton;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.flashlight.client.eclipse.jobs.RefreshRunManagerSLJob;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.model.RunDescription;

/**
 * A singleton that manages the set of run directory aggregates.
 */
@Singleton
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

  private RunManager() {
    f_dataDir = EclipseUtility.getFlashlightDataDirectory();
  }

  private final Set<IRunManagerObserver> f_observers = new CopyOnWriteArraySet<IRunManagerObserver>();

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
  private final Set<RunDirectory> f_runs = new HashSet<RunDirectory>();

  @NonNull
  public Set<RunDirectory> getRunDirectories() {
    synchronized (f_runs) {
      return new HashSet<RunDirectory>(f_runs);
    }
  }

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
   * Gets the set of prepared run descriptions known to this manager.
   * 
   * @return a copy of the set of prepared run descriptions known to this
   *         manager.
   */
  @NonNull
  public Set<RunDescription> getPreparedRunDescriptions() {
    final Set<RunDescription> result = new HashSet<RunDescription>();
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        if (runDir.isPreparedOrIsBeingPrepared())
          result.add(runDir.getDescription());
      }
    }
    return result;
  }

  /**
   * Gets if a run description has been prepared or not.
   * 
   * @param runDescription
   *          a run description.
   * @return {@code true} if the run has been prepared, {@code false} otherwise.
   */
  public boolean isPrepared(final RunDescription runDescription) {
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        if (runDir.getDescription().equals(runDescription)) {
          if (runDir.isPreparedOrIsBeingPrepared())
            return true;
          else
            return false;
        }
      }
      return false;
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
   * Gets the set of run directories managed by this that have not been
   * prepared. The return set can be empty, but will not be {@code null}.
   * 
   * @return the set of run directories managed by this that have not been
   *         prepared. May be empty.
   */
  @NonNull
  public Set<RunDirectory> getNotPreparedRunDirectories() {
    final Set<RunDirectory> result = getRunDirectories();
    result.removeAll(getPreparedRunDirectories());
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
    if (!getRunDescriptions().equals(runs)) {
      isChanged = true;
    }
    if (!getPreparedRunDescriptions().equals(preparedRuns)) {
      isChanged = true;
    }
    if (isChanged) {
      synchronized (f_runs) {
        f_runs.clear();
        f_runs.addAll(runDirs);
      }
    }

    if (isChanged || forceNotify) {
      notifyObservers();
    }
  }

  @NonNull
  private Set<RunDescription> getRunDescriptions() {
    final Set<RunDescription> result = new HashSet<RunDescription>();
    synchronized (f_runs) {
      for (RunDirectory runDir : f_runs) {
        result.add(runDir.getDescription());
      }
    }
    return result;
  }
}
