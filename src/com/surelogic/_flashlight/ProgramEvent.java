package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

/**
 * Holds data about an event which occurred in the instrumented program. This
 * class assumes that it is being called from the same thread that the program
 * event occurred within. Intended to be subclassed for each specific type of
 * event that can occur.
 */
abstract class ProgramEvent extends Event {

	/**
	 * An identity for the thread this event occurred within.
	 */
	private final ThreadPhantomReference f_withinthread = Phantom
			.ofThread(Thread.currentThread());

	PhantomReference getWithinThread() {
		return f_withinthread;
	}

	private final SrcLoc f_location;

	SrcLoc getLocation() {
		return f_location;
	}

	ProgramEvent(final SrcLoc location) {
		if (location == null)
			f_location = SrcLoc.UNKNOWN;
		else
			f_location = location;
	}

	protected final void addThread(final StringBuilder b) {
		Entities.addAttribute("thread", f_withinthread.getId(), b);
		if (f_location != SrcLoc.UNKNOWN) {
			Entities.addAttribute("file", f_location.getFileName(), b);
			Entities.addAttribute("line", f_location.getLine(), b);
		}
	}
}
