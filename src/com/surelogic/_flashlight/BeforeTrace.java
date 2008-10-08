package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.BlockingQueue;

final class BeforeTrace extends Trace implements ICallLocation {	
	BeforeTrace(final long siteId, BlockingQueue<List<Event>> queue) {
		super(siteId);
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
