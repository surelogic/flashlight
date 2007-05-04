package com.surelogic._flashlight;

public final class BeforeIntrinsicLockWait extends IntrinsicLock {

	BeforeIntrinsicLockWait(final Object lockObject, final SrcLoc location) {
		super(lockObject, location);
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<before-intrinsic-lock-wait");
		addNanoTime(b);
		addThread(b);
		addLock(b);
		b.append("/>");
		return b.toString();
	}
}
