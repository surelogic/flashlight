package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ObservedCallLocation extends AbstractCallLocation {
	private static final ConcurrentMap<ObservedCallLocation,ObservedCallLocation> map = 
		new ConcurrentHashMap<ObservedCallLocation, ObservedCallLocation>();
	
	// All negative, since dynamically created;
	private static final AtomicLong nextId = new AtomicLong(0);
	
	private final ClassPhantomReference f_withinClass;
	private final int f_line;
	
	public final long getWithinClassId() {
		return f_withinClass.getId();
	}

	public final int getLine() {
		return f_line;
	}
	
	private ObservedCallLocation(ClassPhantomReference withinClass, int line,
			                     final long siteId) {
	    super(siteId);
	    f_withinClass = withinClass;
	    f_line = line;
	}
	
	static ObservedCallLocation getInstance(ClassPhantomReference withinClass, int line,
			                                BlockingQueue<List<Event>> queue) {
		ObservedCallLocation key = new ObservedCallLocation(withinClass, line, 0);
		ObservedCallLocation loc = map.get(key);
		if (loc == null) {
			final long id = nextId.decrementAndGet();
			loc = new ObservedCallLocation(withinClass, line, id);
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
	
    @Override
    public int hashCode() {
        return (int) (f_withinClass.getId() + f_line);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ObservedCallLocation) {
        	ObservedCallLocation loc = (ObservedCallLocation) o;
            return loc.f_withinClass == f_withinClass &&
                   loc.f_line == f_line;
        }
        return false;
    }
}
