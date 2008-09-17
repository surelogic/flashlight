package com.surelogic._flashlight;

import java.util.concurrent.locks.Lock;

final class AfterUtilConcurrentLockReleaseAttempt extends UtilConcurrentLock {
	/**
	 * <code>true</code> if the acquisition attempt was successful and the
	 * lock was obtained by the thread.
	 */
	private final boolean f_releasedTheLock;

	boolean releasedTheLock() {
		return f_releasedTheLock;
	}

	AfterUtilConcurrentLockReleaseAttempt(final boolean releasedTheLock,
			final Lock lockObject, final ClassPhantomReference withinClass, final int line) {
		super(lockObject, withinClass, line);
		f_releasedTheLock = releasedTheLock;
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<after-util-concurrent-lock-release-attempt");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		Entities.addAttribute("released-the-lock", f_releasedTheLock ? "yes"
				: "no", b);
		b.append("/>");
		return b.toString();
	}
}
