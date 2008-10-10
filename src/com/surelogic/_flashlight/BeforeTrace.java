package com.surelogic._flashlight;

final class BeforeTrace extends Trace implements ICallLocation {	
	BeforeTrace(final long siteId, Store.State state) {
		super(siteId, state);
	}
	
	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<before-trace");
		addNanoTime(b);
		addThread(b);
		b.append("/>");
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return (int) getSiteId();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ObservedCallLocation) {
			ObservedCallLocation bt = (ObservedCallLocation) o;
            return bt.getSiteId() == getSiteId();
 		}
		return false;
	}
}
