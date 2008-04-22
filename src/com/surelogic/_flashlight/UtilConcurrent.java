package com.surelogic._flashlight;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Tracks definitions of {@link ReadWriteLock} instances observed within the
 * instrumented program.
 */
public final class UtilConcurrent {

	static private final Set<IdPhantomReference> f_knownReadWriteLockIds = new HashSet<IdPhantomReference>();

	static void addReadWriteLock(final IdPhantomReference o) {
		synchronized (f_knownReadWriteLockIds) {
			f_knownReadWriteLockIds.add(o);
		}
	}

	static boolean containsReadWriteLock(final IdPhantomReference o) {
		synchronized (f_knownReadWriteLockIds) {
			return f_knownReadWriteLockIds.contains(o);
		}
	}

	static void remove(final IdPhantomReference o) {
		synchronized (f_knownReadWriteLockIds) {
			f_knownReadWriteLockIds.remove(o);
		}
	}

	private UtilConcurrent() {
		// no instances
	}
}
