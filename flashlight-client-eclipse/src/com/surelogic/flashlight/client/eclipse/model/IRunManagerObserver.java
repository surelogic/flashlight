package com.surelogic.flashlight.client.eclipse.model;

public interface IRunManagerObserver {

  void notifyLaunchedRunChange();

  void notifyPrepareDataJobScheduled();

  void notifyCollectionCompletedRunDirectoryChange();
}
