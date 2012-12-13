package com.surelogic.flashlight.client.eclipse.model;

import com.surelogic.Immutable;

@Immutable
public enum RunState {

  INSTRUMENTATION_AND_LAUNCH("Performing instrumentation and launching the application..."),

  COLLECTING_DATA("Collecting data from running application..."),

  STOP_COLLECTION_REQUESTED("Data collection is in the process of shutting down..."),

  DONE_COLLECTING_DATA("Data collection completed"),

  READY("Data ready to be queried...");

  private final String f_label;

  private RunState(String label) {
    f_label = label;
  }

  public String getLabel() {
    return f_label;
  }
}
