package com.surelogic._flashlight.rewriter.engine;

/**
 * Interface used by {@link RewriteEngine} to report status messages.
 */
public interface EngineMessenger {
  public void message(final String message);
}
