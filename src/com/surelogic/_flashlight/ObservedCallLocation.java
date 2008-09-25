package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.*;

public class ObservedCallLocation extends ObservationalEvent implements ICallLocation {
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
	
	private final long f_withinClassId;
	private final int f_line;

	int getLine() {
		return f_line;
	}

	long getWithinClassId() {
		return f_withinClassId;
	}
	
	private ObservedCallLocation(final String fileName, final String locationName,
			                     final long withinClassId, final int line) {
		f_fileName = fileName == null ? "<unknown file name>" : fileName;
		f_locationName = locationName == null ? "<unknown location>"
				: locationName;
		f_withinClassId = withinClassId;
		f_line = line;
	}
	
	static ObservedCallLocation getInstance(BeforeTrace bt, final String fileName, final String locationName,
			                         BlockingQueue<List<Event>> queue) {
		ObservedCallLocation loc = map.get(bt);
		if (loc == null) {
			loc = new ObservedCallLocation(fileName, locationName, bt.getWithinClassId(), bt.getLine());
			ObservedCallLocation last = map.putIfAbsent(loc, loc);
			if (last == null) {
				Store.putInQueue(queue, loc);
			}
		}
		return loc;
	}
	
	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}
	
	@Override
	public int hashCode() {
		return (int) (f_withinClassId + f_line);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof BeforeTrace) {
			BeforeTrace bt = (BeforeTrace) o;
			return bt.getLine() == f_line &&
			       bt.getWithinClassId() == f_withinClassId;
 		}
		return false;
	}
	
	@Override
	public String toString() {
		return "";
	}
}
