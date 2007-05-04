package com.surelogic._flashlight;

/**
 * Abstract base class for all Flashlight events. Intended to be subclassed for
 * each specific type of event that can occur.
 */
abstract class Event {

	/**
	 * The value of <code>System.nanoTime()</code> when this event was
	 * constructed.
	 */
	private final long f_nanoTime = System.nanoTime();

	long getNanoTime() {
		return f_nanoTime;
	}

	/**
	 * Accepts this event on the passed visitor.
	 * 
	 * @param v
	 *            the visitor for this event.
	 */
	abstract void accept(final EventVisitor v);

	protected final void addNanoTime(final StringBuilder b) {
		Entities.addAttribute("nano-time", f_nanoTime, b);
	}
}
