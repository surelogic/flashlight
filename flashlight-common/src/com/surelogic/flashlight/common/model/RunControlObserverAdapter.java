package com.surelogic.flashlight.common.model;

/**
 * This adapter class provides default implementations for the methods described
 * by the {@link IRunControlObserver} interface.
 * <p>
 * Classes that wish to deal with run control changes which occur as
 * Flashlight-instrumented applications are started and collect data can extend
 * this class and override only the methods which they are interested in.
 */
public final class RunControlObserverAdapter implements IRunControlObserver {

  public void runStarting(IDataCollectingRun run) {
    // do nothing
  }

  public void runCollecting(IDataCollectingRun run) {
    // do nothing
  }

  public void runDoneCollecting(IDataCollectingRun run) {
    // do nothing
  }

  public void runControlSomethingChanged() {
    // do nothing
  }
}
