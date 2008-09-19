package com.surelogic._flashlight.instrument;

public interface Log {
  public void log(String message);
  public void log(Throwable throwable);
  public void shutdown();
}
