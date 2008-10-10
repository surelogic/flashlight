package com.surelogic._flashlight;

final class AfterTrace extends Trace {

	AfterTrace(final long siteId, Store.State state) {
		super(siteId, state);
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
