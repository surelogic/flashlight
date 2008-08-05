package com.surelogic._flashlight.rewriter.engine;

public final class ConsoleMessenger extends AbstractIndentingMessager {
  public static final ConsoleMessenger prototype = new ConsoleMessenger();
  
  private ConsoleMessenger() {
    super();
  }
  
  public void error(final int nesting, final String message) {
    System.err.println(indentMessage(nesting, "ERROR: " + message));
  }
  
  public void warning(final int nesting, final String message) {
    System.out.println(indentMessage(nesting, "WARNING: " + message));
  }
  
  public void info(final int nesting, final String message) {
    System.out.println(indentMessage(nesting, message));
  }
}
