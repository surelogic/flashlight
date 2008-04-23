package com.surelogic._flashlight;

import java.util.concurrent.locks.Lock;

public abstract class UtilConcurrentLock extends WithinThreadEvent {

	/**
	 * Phantom reference to the util.concurrent lock object.
	 */
	private final IdPhantomReference f_lockObject;

	IdPhantomReference getLockObject() {
		return f_lockObject;
	}

	UtilConcurrentLock(final Lock lockObject, final SrcLoc location) {
		super(location);
		assert lockObject != null;
		f_lockObject = Phantom.of(lockObject);
	}

	protected final void addLock(final StringBuilder b) {
		Entities.addAttribute("lock", f_lockObject.getId(), b);
	}
}
