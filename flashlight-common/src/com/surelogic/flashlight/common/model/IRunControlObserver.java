package com.surelogic.flashlight.common.model;

/**
 * Implemented to observe changes to running Flashlight-instrumented
 * applications.
 * <p>
 * Classes that wish to deal with run control changes which occur as
 * Flashlight-instrumented applications are started and collect data can
 * implement this interface or they can extend {@link RunControlObserverAdapter}
 * and override only the methods which they are interested in.
 * 
 * @see RunControlObserverAdapter
 * @see RunControlManager
 */
public interface IRunControlObserver {

  /**
   * Notification of a new run.
   * <p>
   * The thread context of this call is not defined and most certainly will not
   * be the UI thread.
   * 
   * @param run
   *          a running Flashlight-instrumented application.
   */
  void runStarting(IDataCollectingRun run);

  /**
   * Notification that a run is now collecting data.
   * <p>
   * The thread context of this call is not defined and most certainly will not
   * be the UI thread.
   * 
   * @param run
   *          a running Flashlight-instrumented application.
   */
  void runCollecting(IDataCollectingRun run);

  /**
   * Notification that a run is finished collecting data. The program may still
   * be running, but data collection has been shut down. The run should be ready
   * to be prepared.
   * <p>
   * The thread context of this call is not defined and most certainly will not
   * be the UI thread.
   * 
   * @param run
   *          a running Flashlight-instrumented application.
   */
  void runDoneCollecting(IDataCollectingRun run);

  /**
   * A general notification called when anything managed by the run manager
   * changes. It is always called after one of the more specific callbacks
   * listed above.
   */
  void runControlSomethingChanged();
}
