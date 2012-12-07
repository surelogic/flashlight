package com.surelogic.flashlight.client.eclipse.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.NonNull;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.Singleton;
import com.surelogic.ThreadSafe;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.flashlight.common.model.DataCollectingRunState;
import com.surelogic.flashlight.common.model.IDataCollectingRun;

/**
 * Singleton manager of launched Flashlight-instrumented applications.
 */
@ThreadSafe
@Singleton
@Region("private RunState")
@RegionLock("RunLock is f_lock protects RunState")
public final class RunControlManager {

  private static final RunControlManager INSTANCE = new RunControlManager();

  /**
   * Gets the singleton instance of the run control manager.
   * 
   * @return the singleton instance of the run control manager.
   */
  public static RunControlManager getInstance() {
    return INSTANCE;
  }

  private RunControlManager() {
    // singleton
    IDataCollectingRun android = new IDataCollectingRun() {

      final Date start = new Date();

      public void stopDataCollectionAsSoonAsPossible() {
      }

      public boolean isAndroid() {
        return true;
      }

      @NonNull
      public String getRunSimpleNameforUI() {
        return "CounterRace";
      }

      @NonNull
      public Date getLaunchTime() {
        return start;
      }
    };

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    IDataCollectingRun java = new IDataCollectingRun() {

      final Date start = new Date();

      public void stopDataCollectionAsSoonAsPossible() {
      }

      public boolean isAndroid() {
        return false;
      }

      @NonNull
      public String getRunSimpleNameforUI() {
        return "PlanetBaronServer";
      }

      @NonNull
      public Date getLaunchTime() {
        return start;
      }
    };

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    IDataCollectingRun java2 = new IDataCollectingRun() {

      final Date start = new Date();

      public void stopDataCollectionAsSoonAsPossible() {
      }

      public boolean isAndroid() {
        return false;
      }

      @NonNull
      public String getRunSimpleNameforUI() {
        return "PlanetBaronServer";
      }

      @NonNull
      public Date getLaunchTime() {
        return start;
      }
    };
    this.runStarting(android);
    this.runCollecting(android);
    this.runStarting(java);
    this.runDoneCollecting(java);
    this.runStarting(java2);
  }

  private final Object f_lock = new Object();

  @UniqueInRegion("RunState")
  private final Map<IDataCollectingRun, DataCollectingRunState> f_runToState = new HashMap<IDataCollectingRun, DataCollectingRunState>();

  /**
   * Gets all runs known to this manager and their current state. The returned
   * list may be freely mutated&mdash;it is a copy.
   * 
   * @return a list of all known to this manager and their current state.
   */
  public ArrayList<Pair<IDataCollectingRun, DataCollectingRunState>> getManagedRuns() {
    final ArrayList<Pair<IDataCollectingRun, DataCollectingRunState>> result = new ArrayList<Pair<IDataCollectingRun, DataCollectingRunState>>();
    synchronized (f_lock) {
      for (Map.Entry<IDataCollectingRun, DataCollectingRunState> entry : f_runToState.entrySet()) {
        result.add(new Pair<IDataCollectingRun, DataCollectingRunState>(entry.getKey(), entry.getValue()));
      }
    }
    return result;
  }

  /**
   * Adds the passed run to this manager. Subsequent calls to change the state
   * of this run should pass the same object instance.
   * <p>
   * Run control manager observers are notified if this method is successful.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * @throws IllegalStateException
   *           if the passed run is known to this manager.
   * @throws IllegalArgumentException
   *           if the passed run is {@code null}.
   */
  public void runStarting(@NonNull IDataCollectingRun run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    synchronized (f_lock) {
      if (f_runToState.containsKey(run))
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " passed to runStarting() more than once");
      f_runToState.put(run, DataCollectingRunState.STARTING);
    }
    notify(run, DataCollectingRunState.STARTING);
  }

  /**
   * Notifies this manager that the passed run is now collecting data. The run
   * must be known to the manager via a subsequent call to
   * {@link #runStarting(IDataCollectingRun)}.
   * <p>
   * Run control manager observers are notified if this method is successful.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * 
   * @throws IllegalStateException
   *           if the passed run is not known to this manager or it is not in
   *           the {@link DataCollectingRunState#STARTING} state.
   * @throws IllegalArgumentException
   *           if the passed run is {@code null}.
   */
  public void runCollecting(@NonNull IDataCollectingRun run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    synchronized (f_lock) {
      DataCollectingRunState state = f_runToState.get(run);
      if (state == null)
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " not known to the "
            + RunControlManager.class.getSimpleName());
      if (state != DataCollectingRunState.STARTING)
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " state must be " + DataCollectingRunState.STARTING
            + " but instead is " + state);
      f_runToState.put(run, DataCollectingRunState.COLLECTING);
    }
    notify(run, DataCollectingRunState.COLLECTING);
  }

  /**
   * Requests that this terminate collection as soon as possible.
   * <p>
   * Run control manager observers are notified if this method is successful.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * @throws IllegalStateException
   *           if the passed run is not known to this manager.
   * @throws IllegalArgumentException
   *           if the passed run is {@code null}.
   */
  public void stopDataCollectionAsSoonAsPossible(@NonNull IDataCollectingRun run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    synchronized (f_lock) {
      DataCollectingRunState state = f_runToState.get(run);
      if (state == null)
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " not known to the "
            + RunControlManager.class.getSimpleName());
      f_runToState.put(run, DataCollectingRunState.STOP_COLLECTION_REQUESTED);
      run.stopDataCollectionAsSoonAsPossible();
    }
    notify(run, DataCollectingRunState.STOP_COLLECTION_REQUESTED);
  }

  /**
   * Notifies this manager that the passed run is finished collecting data. The
   * application may still be running, but Flashlight data collection has been
   * shut down. The run should now be ready to be prepared. The run must be
   * known to the manager via a subsequent call to
   * {@link #runStarting(IDataCollectingRun)}.
   * <p>
   * Note that it is not required that
   * {@link #runCollecting(IDataCollectingRun)} have been invoked for the passed
   * run, just that {@link #runStarting(IDataCollectingRun)} has been invoked. A
   * run is allowed to finish collecting data before it ever started collecting
   * data.
   * <p>
   * Run control manager observers are notified if this method is successful.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * 
   * @throws IllegalStateException
   *           if the passed run is not known to this manager.
   * @throws IllegalArgumentException
   *           if the passed run is {@code null}.
   */
  public void runDoneCollecting(@NonNull IDataCollectingRun run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    synchronized (f_lock) {
      DataCollectingRunState state = f_runToState.get(run);
      if (state == null)
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " not known to the "
            + RunControlManager.class.getSimpleName());
      f_runToState.put(run, DataCollectingRunState.FINISHED);
    }
    notify(run, DataCollectingRunState.FINISHED);
  }

  /**
   * Clears out runs that have reached the
   * {@link DataCollectingRunState#FINISHED} state.
   * <p>
   * Run control manager observers are notified if this method, in face, cleared
   * out any runs. Notification is skipped if this manager had no finished runs.
   */
  public void clearAllFinishedRuns() {
    final Set<IDataCollectingRun> cleared = new HashSet<IDataCollectingRun>();
    synchronized (f_lock) {
      for (Iterator<Map.Entry<IDataCollectingRun, DataCollectingRunState>> iterator = f_runToState.entrySet().iterator(); iterator
          .hasNext();) {
        Map.Entry<IDataCollectingRun, DataCollectingRunState> entry = iterator.next();
        if (entry.getValue() == DataCollectingRunState.FINISHED) {
          cleared.add(entry.getKey());
          iterator.remove();
        }
      }
    }
    if (!cleared.isEmpty())
      notifyCleared(cleared);
  }

  /**
   * Clears the passed run that must have reached the
   * {@link DataCollectingRunState#FINISHED} state. The run must be known to the
   * manager via a subsequent call to {@link #runStarting(IDataCollectingRun)}.
   * <p>
   * Run control manager observers are notified if this method is successful.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * 
   * @throws IllegalStateException
   *           if the passed run is not known to this manager or it is not in
   *           the {@link DataCollectingRunState#FINISHED} state.
   * @throws IllegalArgumentException
   *           if the passed run is {@code null}.
   */
  public void clearFinishedRun(@NonNull final IDataCollectingRun run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    synchronized (f_lock) {
      DataCollectingRunState state = f_runToState.get(run);
      if (state == null)
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " not known to the "
            + RunControlManager.class.getSimpleName());
      if (state != DataCollectingRunState.FINISHED)
        throw new IllegalStateException(run.getRunSimpleNameforUI() + " state must be " + DataCollectingRunState.FINISHED
            + " but instead is " + state);
      f_runToState.remove(run);
    }
    final Set<IDataCollectingRun> cleared = new HashSet<IDataCollectingRun>();
    cleared.add(run);
    notifyCleared(cleared);
  }

  @Vouch("AnnotationBounds")
  private final CopyOnWriteArrayList<IRunControlObserver> f_observers = new CopyOnWriteArrayList<IRunControlObserver>();

  /**
   * Registers an observer of this manager.
   * 
   * @param observer
   *          a run control observer.
   */
  public void register(IRunControlObserver observer) {
    if (observer != null)
      f_observers.add(observer);
  }

  /**
   * Removes an observer of this manager.
   * 
   * @param observer
   *          a run control observer.
   * @return {@code true} if the observers contained the specified element.
   */
  public boolean remove(IRunControlObserver observer) {
    if (observer != null)
      return f_observers.remove(observer);
    else
      return false;
  }

  /**
   * Notifies observers of a change to a run.
   * <p>
   * Callers must not hold a lock when invoking this method for fear of deadlock
   * occurring.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * @param newState
   *          the new state of the passed run.
   */
  private void notify(@NonNull final IDataCollectingRun run, @NonNull final DataCollectingRunState newState) {
    for (final IRunControlObserver observer : f_observers) {
      observer.launchedRunStateChanged(run, newState);
    }
  }

  /**
   * Notifies observers that that the set of runs in the
   * {@link DataCollectingRunState#FINISHED} state has been cleared.
   */
  private void notifyCleared(@NonNull final Set<IDataCollectingRun> cleared) {
    for (final IRunControlObserver observer : f_observers) {
      observer.launchedRunCleared(cleared);
    }
  }
}