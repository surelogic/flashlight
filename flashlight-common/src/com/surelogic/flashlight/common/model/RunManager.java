package com.surelogic.flashlight.common.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.NonNull;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.files.RawFileUtility;
import com.surelogic.flashlight.common.files.RunDirectory;
import com.surelogic.flashlight.common.jobs.RefreshRunManagerSLJob;

/**
 * A singleton that manages the set of run description aggregates. This class
 * will not be populated with data until its {@link #refresh()} method is
 * invoked.
 */
public final class RunManager {

  /**
   * Initializes the run manager with the location of the Flashlight data
   * directory. This is necessary because this directory is only known to
   * EclipseUtility (it is workspace relative) and this class cannot directly
   * invoke that utilitie's code. The Flashlight client activator should be the
   * only code to invoke this method.
   * 
   * @param flashlightDataDir
   *          the Flashlight data directory.
   * @throws IllegalStateException
   *           if called more than once.
   * @throws IllegalArgumentException
   *           if the passed directory is null.
   */
  public static void initialize(File flashlightDataDir) {
    if (INSTANCE.get() != null)
      throw new IllegalStateException(I18N.err(168));
    final RunManager singleton = new RunManager(flashlightDataDir);
    INSTANCE.set(singleton);
  }

  private static final AtomicReference<RunManager> INSTANCE = new AtomicReference<RunManager>();

  /**
   * Gets the singleton instance.
   * 
   * @return the singleton instance.
   * @throws IllegalStateException
   *           if the run manager has not been initialized with the location of
   *           the Flashlight data directory from Eclipse.
   */
  @NonNull
  public static RunManager getInstance() {
    final RunManager singleton = INSTANCE.get();
    if (singleton != null)
      return singleton;
    else
      throw new IllegalStateException(I18N.err(167));
  }

  private RunManager(File flashlightDataDir) {
    if (flashlightDataDir == null)
      throw new IllegalArgumentException(I18N.err(44, "flashlightDataDir"));
    f_dataDir = flashlightDataDir;
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
   * Holds the set of all known run descriptions.
   */
  private final AtomicReference<Set<RunDescription>> f_runs = new AtomicReference<Set<RunDescription>>(
      new HashSet<RunDescription>());

  /**
   * Holds the set of all known prepared run descriptions.
   */
  private final AtomicReference<Set<RunDescription>> f_preparedRuns = new AtomicReference<Set<RunDescription>>(
      new HashSet<RunDescription>());

  /**
   * Gets the set of run descriptions known to this manager.
   * 
   * @return the non-null set of run descriptions known to this manager. This is
   *         a copy of the set maintained by this manager so it can be freely
   *         mutated by callers.
   */
  public Set<RunDescription> getRunDescriptions() {
    return new HashSet<RunDescription>(f_runs.get());
  }

  /**
   * Gets the set of prepared run descriptions known to this manager.
   * 
   * @return the non-null set of prepared run descriptions known to this
   *         manager. This is a copy of the set maintained by this manager so it
   *         can be freely mutated by callers.
   */
  public Set<RunDescription> getPreparedRunDescriptions() {
    return new HashSet<RunDescription>(f_preparedRuns.get());
  }

  /**
   * Gets if a run description has been prepared or not.
   * 
   * @param runDescription
   *          a run description.
   * @return {@code true} if the run has been prepared, {@code false} otherwise.
   */
  public boolean isPrepared(final RunDescription runDescription) {
    return f_preparedRuns.get().contains(runDescription);
  }

  /**
   * Gets an array containing the identity strings of all run descriptions known
   * to this manager.
   * 
   * @return the identity strings of all run descriptions known to this manager.
   */
  public String[] getRunIdentities() {
    final Set<RunDescription> descs = getRunDescriptions();
    final Set<String> ids = new HashSet<String>(descs.size());
    for (final RunDescription r : descs) {
      ids.add(r.toIdentityString());
    }
    return ids.toArray(SLUtility.EMPTY_STRING_ARRAY);
  }

  /**
   * Looks up a run with a given identity string. Results in {@code null} if no
   * such run can be found.
   * 
   * @param idString
   *          an identity string.
   * @return a run with <tt>idString</tt>, or {@code null} if no such run can be
   *         found.
   */
  public RunDescription getRunByIdentityString(final String idString) {
    for (final RunDescription runDescription : f_runs.get()) {
      if (runDescription.toIdentityString().equals(idString)) {
        return runDescription;
      }
    }
    return null;
  }

  /**
   * Gets the set of run descriptions known to this manager that have not been
   * prepared. This set can be empty, but will not be {@code null}.
   * 
   * @return the non-null set of run descriptions known to this manager that
   *         have not been prepared. This is a copy of the set maintained by
   *         this manager so it can be freely mutated by callers.
   */
  public Set<RunDescription> getUnPreppedRunDescriptions() {
    final Set<RunDescription> result = getRunDescriptions();
    result.removeAll(getPreparedRunDescriptions());
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
    /*
     * Put in all the raw descriptions with a null. This indicates that there is
     * no prepared description associated with them.
     */
    for (final RunDirectory dir : runDirs) {
      final RunDescription key = dir.getRunDescription();
      // Assume the run is not prepped until we find out otherwise
      runs.add(key);
      final File dbDir = dir.getDatabaseDirectory();
      if (dbDir.exists()) {
        // Check to see if there is a prepped run
        preparedRuns.add(key);
      }
    }

    /*
     * Check if anything changed...if so, update the fields and notify
     * observers.
     */
    if (!f_runs.get().equals(runs)) {
      isChanged = true;
      f_runs.set(Collections.unmodifiableSet(runs));
    }
    if (!f_preparedRuns.get().equals(preparedRuns)) {
      isChanged = true;
      f_preparedRuns.set(Collections.unmodifiableSet(preparedRuns));
    }

    if (isChanged || forceNotify) {
      notifyObservers();
    }
  }
}
