package com.surelogic._flashlight;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Tracks definitions of {@link ReadWriteLock} instances observed within the
 * instrumented program.
 */
public final class UtilConcurrent {

	private final Set<IdPhantomReference> f_knownReadWriteLockIds = new HashSet<IdPhantomReference>();

	/**
	 * Adds a ReadWriteLock reference to the set of known instances.
	 * 
	 * @param o
	 *            the reference to add.
	 * @return {@code true} if this set did not already contain the specified
	 *         element
	 */
	public boolean addReadWriteLock(final IdPhantomReference o) {
		synchronized (f_knownReadWriteLockIds) {
			return f_knownReadWriteLockIds.add(o);
		}
	}

	void remove(final IdPhantomReference o) {
		synchronized (f_knownReadWriteLockIds) {
			f_knownReadWriteLockIds.remove(o);
		}
	}

}
