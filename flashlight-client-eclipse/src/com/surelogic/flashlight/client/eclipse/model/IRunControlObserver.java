package com.surelogic.flashlight.client.eclipse.model;

import java.util.Set;

import com.surelogic.flashlight.common.model.DataCollectingRunState;
import com.surelogic.flashlight.common.model.IDataCollectingRun;

/**
 * Implemented to observe changes to launched Flashlight-instrumented
 * applications.
 * <p>
 * Classes that wish to deal with run control changes which occur as
 * Flashlight-instrumented applications are started and collect data can
 * implement this interface.
 * 
 * @see RunControlManager
 */
public interface IRunControlObserver {

  /**
   * Notification of a change to the state of a launched Flashlight-instrumented
   * application.
   * 
   * @param run
   *          a launched Flashlight-instrumented application.
   * @param newState
   *          the new state of the passed run.
   */
  void launchedRunStateChanged(IDataCollectingRun run, DataCollectingRunState newState);

  /**
   * Notification that the passed set of runs in the
   * {@link DataCollectingRunState#FINISHED} state has been cleared.
   */
  void launchedRunCleared(Set<IDataCollectingRun> cleared);
}