package com.surelogic._flashlight;

import java.util.concurrent.locks.Lock;

final class AfterUtilConcurrentLockAcquisitionAttempt extends
		UtilConcurrentLock {

	/**
	 * <code>true</code> if the acquisition attempt was successful and the lock
	 * was obtained by the thread.
	 */
	private final boolean f_gotTheLock;

	boolean gotTheLock() {
		return f_gotTheLock;
	}

	AfterUtilConcurrentLockAcquisitionAttempt(final boolean gotTheLock,
			final Lock lockObject, final long siteId,
			final PostMortemStore.State state) {
		super(lockObject, siteId, state);
		f_gotTheLock = gotTheLock;
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<after-util-concurrent-lock-acquisition-attempt");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		Entities.addAttribute("got-the-lock", f_gotTheLock ? "yes" : "no", b);
		b.append("/>");
		return b.toString();
	}
}
