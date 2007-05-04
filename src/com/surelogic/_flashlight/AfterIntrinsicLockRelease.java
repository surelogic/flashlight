package com.surelogic._flashlight;

public final class AfterIntrinsicLockRelease extends IntrinsicLock {

	AfterIntrinsicLockRelease(final Object lockObject, final SrcLoc location) {
		super(lockObject, location);
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<after-intrinsic-lock-release");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		b.append("/>");
		return b.toString();
	}
}
