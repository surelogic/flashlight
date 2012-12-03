package com.surelogic._flashlight.rewriter;

import java.io.PrintWriter;

public final class PrintWriterMessenger extends AbstractIndentingMessager {

  private final PrintWriter printWriter;

  /**
   * Constructs a new messenger using the passed {@link PrintWriter}. Client is
   * responsible for flushing and/or closing the passed stream.
   * 
   * @param pw
   *          to write output to.
   */
  public PrintWriterMessenger(final PrintWriter pw) {
    printWriter = pw;
  }

  @Override
  protected void output(String line) {
    printWriter.println(line);
  }
}
