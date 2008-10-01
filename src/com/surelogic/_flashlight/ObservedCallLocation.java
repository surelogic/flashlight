package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.*;

public class ObservedCallLocation extends AbstractCallLocation {
	private static final ConcurrentMap<ICallLocation,ObservedCallLocation> map = 
		new ConcurrentHashMap<ICallLocation, ObservedCallLocation>();
		
	private final String f_fileName;

	String getDeclaringTypeName() {
		return f_fileName;
	}

	private final String f_locationName;

	String getLocationName() {
		return f_locationName;
	}
	
	private ObservedCallLocation(final String fileName, final String locationName,
			                     final ClassPhantomReference inClass, final int line) {
	    super(inClass, line);
		f_fileName = fileName == null ? "<unknown file name>" : fileName;
		f_locationName = locationName == null ? "<unknown location>"
				: locationName;
	}
	
	static ObservedCallLocation getInstance(BeforeTrace bt, final String fileName, final String locationName,
			                         BlockingQueue<List<Event>> queue) {
		ObservedCallLocation loc = map.get(bt);
		if (loc == null) {
			loc = new ObservedCallLocation(fileName, locationName, bt.getWithinClass(), bt.getLine());
			ObservedCallLocation last = map.putIfAbsent(loc, loc);
			if (last == null) {
				Store.putInQueue(queue, loc);
			}
		}
		return loc;
	}
	
	@Override
	protected void accept(EventVisitor v) {
		v.visit(this);
	}
}
