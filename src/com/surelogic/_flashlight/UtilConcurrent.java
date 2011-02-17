package com.surelogic._flashlight;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Tracks definitions of {@link ReadWriteLock} instances observed within the
 * instrumented program.
 */
public final class UtilConcurrent {

	private final Set<IdPhantomReference> f_knownReadWriteLockIds = Collections
			.newSetFromMap(new ConcurrentHashMap<IdPhantomReference, Boolean>());

	/**
	 * Adds a ReadWriteLock reference to the set of known instances.
	 * 
	 * @param o
	 *            the reference to add.
	 * @return {@code true} if this set did not already contain the specified
	 *         element
	 */
	public boolean addReadWriteLock(final IdPhantomReference o) {
		return f_knownReadWriteLockIds.add(o);
	}

	void remove(final IdPhantomReference o) {
		f_knownReadWriteLockIds.remove(o);
	}

}
