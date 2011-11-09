package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;

public final class PrintWriterMessenger extends AbstractIndentingMessager {
  public static final PrintWriterMessenger console = new PrintWriterMessenger(new PrintWriter(System.out));
  
  private final PrintWriter printWriter;
  
  public PrintWriterMessenger(final PrintWriter pw) {
    printWriter = pw;
  }
  
  public void error(final String message) {
    printWriter.println(indentMessage("ERROR: " + message));
  }
  
  public void warning(final String message) {
    printWriter.println(indentMessage("WARNING: " + message));
  }
  
  public void verbose(final String message) {
    printWriter.println(indentMessage(message));
  }
  
  public void info(final String message) {
    printWriter.println(indentMessage(message));
  }
}
