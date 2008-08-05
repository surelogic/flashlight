package com.surelogic._flashlight.rewriter.engine;

/**
 * Interface used by {@link RewriteEngine} to report status messages.
 */
public interface EngineMessenger {
  public void error(int nesting, String message);
  public void warning(int nesting, String message);
  public void info(int nesting, String message);
}
