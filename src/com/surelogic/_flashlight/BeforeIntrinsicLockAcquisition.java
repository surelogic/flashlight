package com.surelogic._flashlight;

final class BeforeIntrinsicLockAcquisition extends IntrinsicLock {

	/**
	 * <code>true</code> if the lock object is dynamically the same as the
	 * receiver object.
	 */
	private final boolean f_lockIsThis;

	boolean isLockThis() {
		return f_lockIsThis;
	}

	/**
	 * <code>true</code> if the lock object is dynamically the same as the
	 * class the method is declared within.
	 */
	private final boolean f_lockIsClass;

	boolean isLockClass() {
		return f_lockIsClass;
	}

	BeforeIntrinsicLockAcquisition(final Object lockObject,
			final boolean lockIsThis, final boolean lockIsClass,
			final long siteId, Store.State state) {
		super(lockObject, siteId, state);
		f_lockIsThis = lockIsThis;
		f_lockIsClass = lockIsClass;
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<before-intrinsic-lock-acquisition");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		if (f_lockIsThis)
			Entities.addAttribute("lock-is-this", "yes", b);
		if (f_lockIsClass)
			Entities.addAttribute("lock-is-class", "yes", b);
		b.append("/>");
		return b.toString();
	}
}
