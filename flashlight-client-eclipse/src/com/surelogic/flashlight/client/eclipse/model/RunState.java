package com.surelogic.flashlight.client.eclipse.model;

import java.util.EnumSet;

import com.surelogic.Immutable;

@Immutable
public enum RunState {

  INSTRUMENTATION_AND_LAUNCH("Performing instrumentation and launching the application..."),

  COLLECTING_DATA("Collecting data from the instrumented application..."),

  STOP_COLLECTION_REQUESTED("Waiting for data collection to stop..."),

  DONE_COLLECTING_DATA("Data collection complete"),

  READY("Data ready to be queried");

  private final String f_label;

  private RunState(String label) {
    f_label = label;
  }

  public String getLabel() {
    return f_label;
  }

  public static final EnumSet<RunState> IS_FINISHED_COLLECTING_DATA = EnumSet.of(RunState.DONE_COLLECTING_DATA, RunState.READY);
}
