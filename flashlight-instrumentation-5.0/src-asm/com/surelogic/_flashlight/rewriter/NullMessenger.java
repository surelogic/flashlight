package com.surelogic._flashlight.rewriter;

public final class NullMessenger implements RewriteMessenger {
  public static final NullMessenger prototype = new NullMessenger();

  private NullMessenger() {
    super();
  }

  public void increaseNesting() {
    // Do nothing
  }

  public void increaseNestingWith(String messageOutputOnlyIfNestedMessage) {
    // Do nothing
  }

  public void decreaseNesting() {
    // Do nothing
  }

  public void msg(Level level, String message) {
    // Do nothing
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

  public void verbose(final String message) {
    // Do nothing
  }
}
