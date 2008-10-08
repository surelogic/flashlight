package com.surelogic._flashlight;

import java.util.concurrent.locks.Lock;

final class BeforeUtilConcurrentLockAcquisitionAttempt extends
		UtilConcurrentLock {

	BeforeUtilConcurrentLockAcquisitionAttempt(final Lock lockObject, final long siteId) {
		super(lockObject, siteId);
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<before-util-concurrent-lock-acquisition-attempt");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		b.append("/>");
		return b.toString();
	}
}
