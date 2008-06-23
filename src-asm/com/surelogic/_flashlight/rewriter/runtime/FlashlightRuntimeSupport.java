package com.surelogic._flashlight.rewriter.runtime;

/**
 * Helper methods to support Flashlight transformations at runtime.
 * 
 * @author aarong
 */
public final class FlashlightRuntimeSupport {
  private static Log theLog = new Log() {
    public void log(final String message) {
      System.err.println(message);
    }
    
    public void log(final Throwable throwable) {
      throwable.printStackTrace(System.err);
    }
    
    public void shutdown() {
      // nothing to do
    }
  };
  
  
  
  /** Private method to prevent instantiation. */
  private FlashlightRuntimeSupport() {
    // Do nothing
  }
  
  
  
  public static synchronized void setLog(final Log newLog) {
    theLog = newLog;
  }
  
  
  
  /**
   * A fatal error was encountered.
   * @param e The exception reporting the error.
   */
  public static synchronized void reportFatalError(final Exception e) {
    if (theLog != null) {
      theLog.log(e);
    }
  }
}
