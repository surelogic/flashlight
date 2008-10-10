package com.surelogic._flashlight;

abstract class Lock extends TracedEvent {
	/**
	 * Phantom reference of the object being synchronized on.
	 */
	private final IdPhantomReference f_lockObject;

	final IdPhantomReference getLockObject() {
		return f_lockObject;
	}

	Lock(final Object lockObject, final long siteId, Store.State state) {
		super(siteId, state);
		assert lockObject != null;
		f_lockObject = Phantom.of(lockObject);
	}

	protected final void addLock(final StringBuilder b) {
		Entities.addAttribute("lock", f_lockObject.getId(), b);
	}
}
