package com.surelogic._flashlight;

import com.surelogic._flashlight.trace.TraceNode;

abstract class Lock extends TracedEvent {
	/**
	 * Phantom reference of the object being synchronized on.
	 */
	private final IdPhantomReference f_lockObject;

	final IdPhantomReference getLockObject() {
		return f_lockObject;
	}

	Lock(final Object lockObject, final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line, TraceNode.getThreadState());
		assert lockObject != null;
		f_lockObject = Phantom.of(lockObject);
	}

	protected final void addLock(final StringBuilder b) {
		Entities.addAttribute("lock", f_lockObject.getId(), b);
	}
}
