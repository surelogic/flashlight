package com.surelogic._flashlight.rewriter.runtime;

/**
 * Helper methods to support Flashlight transformations at runtime.
 * 
 * @author aarong
 */
public class FlashlightRuntimeSupport {
  /** Private method to prevent instantiation. */
  private FlashlightRuntimeSupport() {
    // Do nothing
  }
  
  
  
  /**
   * A fatal error was encountered.
   * @param e The exception reporting the error.
   */
  public static void reportFatalError(final Exception e) {
    e.printStackTrace(System.err);
  }
}
