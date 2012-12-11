package com.surelogic.flashlight.client.eclipse.model;

public interface IRunManagerObserver {

  void notifyInstrumentedApplicationChange();

  void notifyPrepareDataJobScheduled();

  void notifyCollectionCompletedRunDirectoryChange();
}
