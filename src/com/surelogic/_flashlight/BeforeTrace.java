package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.BlockingQueue;

final class BeforeTrace extends Trace implements ICallLocation {
	private final ObservedCallLocation f_location;
	
	String getDeclaringTypeName() {
		return f_location.getDeclaringTypeName();
	}

	String getLocationName() {
		return f_location.getLocationName();
	}

	BeforeTrace(final String fileName, final String locationName,
			    final ClassPhantomReference withinClass, final int line, BlockingQueue<List<Event>> queue) {
		super(withinClass, line);
		f_location = ObservedCallLocation.getInstance(this, fileName, locationName, queue);
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
		Entities.addAttribute("location", getLocationName(), b);
		Entities.addAttribute("file", getDeclaringTypeName(), b);
		b.append("/>");
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return (int) (getWithinClassId() + getLine());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ObservedCallLocation) {
			ObservedCallLocation bt = (ObservedCallLocation) o;
			return bt.getLine() == getLine() &&
			       bt.getWithinClassId() == getWithinClassId();
 		}
		return false;
	}
}
