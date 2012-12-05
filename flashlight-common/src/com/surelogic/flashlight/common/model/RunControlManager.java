package com.surelogic.flashlight.common.model;

import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.Singleton;
import com.surelogic.ThreadSafe;

@ThreadSafe
@Singleton
public final class RunControlManager {

  private static final RunControlManager INSTANCE = new RunControlManager();

  public static RunControlManager getInstance() {
    return INSTANCE;
  }

  private RunControlManager() {
    // singleton
  }

  /**
   * Adds the passed run to this manager. Subsequent calls should pass the same
   * run.
   * 
   * @param run
   *          a running Flashlight-instrumented application.
   */
  public void runStarting(IDataCollectingRun run) {
    // TODO
  }

  /**
   * Notifies this manager that the passed run is now collecting data. The run
   * must be known to the manager via a subsequent call to
   * {@link #runStarting(IDataCollectingRun)}.
   * 
   * @param run
   *          a running Flashlight-instrumented application.
   * 
   * @throws IllegalStateException
   *           if the passed run is not known to this manager.
   */
  public void runCollecting(IDataCollectingRun run) {
    // TODO
  }

  /**
   * Notifies this manager that the passed run is finished collecting data. The
   * program may still be running, but data collection has been shut down. The
   * run should be ready to be prepared. The run must be known to the manager
   * via a subsequent call to {@link #runStarting(IDataCollectingRun)}.
   * 
   * @param run
   *          a running Flashlight-instrumented application.
   * 
   * @throws IllegalStateException
   *           if the passed run is not known to this manager.
   */
  public void runDoneCollecting(IDataCollectingRun run) {
    // TODO
  }

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
   * Removes an observer from this manager.
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

}
