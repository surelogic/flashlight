package com.surelogic.flashlight.common.model;

public enum DataCollectingRunState {

  STARTING("Launching the Flashlight-instrumented application..."),

  COLLECTING("Collecting data from running application..."),

  STOP_COLLECTION_REQUESTED("Data collection is in the process of shutting down..."),

  FINISHED("Data collection completed");

  private final String f_label;

  private DataCollectingRunState(String label) {
    f_label = label;
  }

  public String getLabel() {
    return f_label;
  }
}
