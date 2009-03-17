package com.surelogic._flashlight.rewriter;

/**
 * Interface used by {@link RewriteEngine} to report status messages.
 */
public interface RewriteMessenger {
  public void increaseNesting();
  public void decreaseNesting();
  public void error(String message);
  public void warning(String message);
  public void verbose(String message);
  public void info(String message);
}
