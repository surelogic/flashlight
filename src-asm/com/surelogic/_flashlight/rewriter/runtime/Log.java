package com.surelogic._flashlight.rewriter.runtime;

public interface Log {
  public void log(String message);
  public void log(Throwable throwable);
  public void shutdown();
}
