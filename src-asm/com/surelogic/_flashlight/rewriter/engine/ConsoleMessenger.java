package com.surelogic._flashlight.rewriter.engine;

public final class ConsoleMessenger implements EngineMessenger {
  public static final ConsoleMessenger prototype = new ConsoleMessenger();
  
  private ConsoleMessenger() {
    super();
  }
  
  public void message(String message) {
    System.out.println(message);
  }
}
