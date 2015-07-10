package com.surelogic._flashlight;

public abstract class TimedEvent extends Event {

  TimedEvent(final long nanoTime) {
    f_nanoTime = nanoTime;
  }

  private final long f_nanoTime;

  public long getNanoTime() {
    return f_nanoTime;
  }

  protected final void addNanoTime(final StringBuilder b) {
    Entities.addAttribute("nano-time", f_nanoTime, b);
  }
}
