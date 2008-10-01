package com.surelogic._flashlight;

public abstract class TimedEvent extends Event {
	/**
	 * The value of <code>System.nanoTime()</code> when this event was
	 * constructed.
	 */
	private final long f_nanoTime = System.nanoTime();

	long getNanoTime() {
		return f_nanoTime;
	}
	
	protected final void addNanoTime(final StringBuilder b) {
		Entities.addAttribute("nano-time", getNanoTime(), b);
	}
}
