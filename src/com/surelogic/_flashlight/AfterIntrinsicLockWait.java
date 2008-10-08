package com.surelogic._flashlight;

final class AfterIntrinsicLockWait extends IntrinsicLock {

	AfterIntrinsicLockWait(final Object lockObject, final long siteId) {
		super(lockObject, siteId);
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<after-intrinsic-lock-wait");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		b.append("/>");
		return b.toString();
	}
}
