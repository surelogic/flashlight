package com.surelogic._flashlight;

abstract class IntrinsicLock extends WithinThreadEvent {

	/**
	 * Phantom reference of the object being synchronized on.
	 */
	private final IdPhantomReference f_lockObject;

	IdPhantomReference getLockObject() {
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
