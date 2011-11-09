package com.surelogic._flashlight.rewriter.runtime;

/**
 * Code to throw this error is inserted by instrumentation at locations where
 * it is statically determined that Flashlight does not have enough information
 * to operate properly due to an incomplete classpath specification during the
 * instrumentation process.  If the application is executed with the same 
 * incomplete classpath that was provided to the instrumentation then this 
 * error is thrown only when the application would otherwise fail with a 
 * {@link ClassNotFoundException} or {@link NoSuchFieldException}.
 */
public class FlashlightRuntimeError extends Error {
  public FlashlightRuntimeError(final String msg) {
    super(msg);
  }
}
