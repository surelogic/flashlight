package com.surelogic._flashlight.rewriter;

/**
 * Interface used by {@link RewriteEngine} to report status messages.
 */
public interface RewriteMessenger {

  public enum Level {
    Verbose, Info, Warning, Error
  }

  void increaseNestingWith(String messageOutputOnlyIfNestedMessage);

  void increaseNesting();

  void decreaseNesting();

  void msg(Level level, String message);

  /*
   * Convenience methods
   */

  void error(String message);

  void warning(String message);

  void info(String message);

  void verbose(String message);
}
