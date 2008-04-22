package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

/**
 * Holds data about an event which occurred within a thread within the
 * instrumented program. This class assumes that it is being constructed within
 * the same thread that the event occurred within.
 * <p>
 * Intended to be subclassed for each specific type of event that can occur.
 */
abstract class WithinThreadEvent extends ProgramEvent {

	/**
	 * An identity for the thread this event occurred within.
	 */
	private final ThreadPhantomReference f_withinThread = Phantom
			.ofThread(Thread.currentThread());

	PhantomReference getWithinThread() {
		return f_withinThread;
	}

	private final SrcLoc f_location;

	SrcLoc getLocation() {
		return f_location;
	}

	WithinThreadEvent(final SrcLoc location) {
		if (location == null)
			f_location = SrcLoc.UNKNOWN;
		else
			f_location = location;
	}

	protected final void addThread(final StringBuilder b) {
		Entities.addAttribute("thread", f_withinThread.getId(), b);
		Entities.addAttribute("in", f_location.getTypeName(), b);
		Entities.addAttribute("line", f_location.getLine(), b);
	}
}
