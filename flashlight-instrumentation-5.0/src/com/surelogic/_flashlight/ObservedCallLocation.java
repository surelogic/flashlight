package com.surelogic._flashlight;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class ObservedCallLocation extends AbstractCallLocation {
	private static final ConcurrentMap<ObservedCallLocation, ObservedCallLocation> map = new ConcurrentHashMap<ObservedCallLocation, ObservedCallLocation>();

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

	private ObservedCallLocation(final ClassPhantomReference withinClass,
			final int line, final long siteId) {
		super(siteId);
		f_withinClass = withinClass;
		f_line = line;
	}

	static ObservedCallLocation getInstance(
			final ClassPhantomReference withinClass, final int line,
			final PostMortemStore.State state) {
		ObservedCallLocation key = new ObservedCallLocation(withinClass, line,
				0);
		ObservedCallLocation loc = map.get(key);
		if (loc == null) {
			final long id = nextId.decrementAndGet();
			loc = new ObservedCallLocation(withinClass, line, id);
			ObservedCallLocation last = map.putIfAbsent(loc, loc);
			if (last == null) {
				PostMortemStore.putInQueue(state, loc);
			}
		}
		return loc;
	}

	@Override
	protected void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public int hashCode() {
		return (int) (f_withinClass.getId() + f_line);
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof ObservedCallLocation) {
			ObservedCallLocation loc = (ObservedCallLocation) o;
			return loc.f_withinClass == f_withinClass && loc.f_line == f_line;
		}
		return false;
	}
}
