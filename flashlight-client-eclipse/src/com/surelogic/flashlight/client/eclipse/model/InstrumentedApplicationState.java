package com.surelogic.flashlight.client.eclipse.model;

public enum InstrumentedApplicationState {

  INSTRUMENTATION_AND_LAUNCH("Performing instrumentation and launching the application..."),

  COLLECTING_DATA("Collecting data from running application..."),

  STOP_COLLECTION_REQUESTED("Data collection is in the process of shutting down..."),

  DONE_COLLECTING_DATA("Data collection completed"),

  PREPARING_DATA("Preparing collected data for querying..."),

  READY("Data ready to be queried...");

  private final String f_label;

  private InstrumentedApplicationState(String label) {
    f_label = label;
  }

  public String getLabel() {
    return f_label;
  }
}
