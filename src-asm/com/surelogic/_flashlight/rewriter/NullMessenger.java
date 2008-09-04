package com.surelogic._flashlight.rewriter.engine;

public final class NullMessenger implements EngineMessenger {
  public static final NullMessenger prototype = new NullMessenger();
  
  private NullMessenger() {
    super();
  }
  
  public void error(final String message) {
    // Do nothing
  }
  
  public void warning(final String message) {
    // Do nothing
  }
  
  public void info(final String message) {
    // Do nothing
  }

  public void decreaseNesting() {
    // Do nothing
  }

  public void increaseNesting() {
    // Do nothing
  }
}
