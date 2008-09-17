package com.surelogic._flashlight;

final class AfterTrace extends Trace {

	AfterTrace(final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line);
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<after-trace");
		addNanoTime(b);
		addThread(b);
		b.append("/>");
		return b.toString();
	}
}
