package com.surelogic._flashlight.instrument;

import com.surelogic._flashlight.rewriter.runtime.Log;

public final class NullLog implements Log {
  public static final Log prototype = new NullLog();
  
  
  
  private NullLog() {
    // do nothing
  }
  
  

  public void log(String message) {
    // do nothing
  }

  public void log(Throwable throwable) {
    // do nothing
  }

  public void shutdown() {
    // do nothing
  }
}
