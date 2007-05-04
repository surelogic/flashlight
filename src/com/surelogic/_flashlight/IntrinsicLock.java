package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

abstract class IntrinsicLock extends ProgramEvent {

	/**
	 * The object being synchronized on.
	 */
	private final IdPhantomReference f_lockObject;

	PhantomReference getLockObject() {
		return f_lockObject;
	}

	IntrinsicLock(final Object lockObject, final SrcLoc location) {
		super(location);
		assert lockObject != null;
		f_lockObject = Phantom.of(lockObject);
	}

	protected final void addLock(final StringBuilder b) {
		Entities.addAttribute("lock", f_lockObject.getId(), b);
	}
}
