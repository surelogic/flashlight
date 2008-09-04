package com.surelogic._flashlight.rewriter;

public final class ConsoleMessenger extends AbstractIndentingMessager {
  public static final ConsoleMessenger prototype = new ConsoleMessenger();
  
  private ConsoleMessenger() {
    super();
  }
  
  public void error(final String message) {
    System.err.println(indentMessage("ERROR: " + message));
  }
  
  public void warning(final String message) {
    System.out.println(indentMessage("WARNING: " + message));
  }
  
  public void info(final String message) {
    System.out.println(indentMessage(message));
  }
}
