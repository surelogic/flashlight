package com.surelogic._flashlight;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which threads have had trace information started for them.
 * <p>
 * The store has to manufacture the first step of the trace from an actual stack
 * trace. This is because the instrumentation can't see the thread start (or the
 * operating system call to start the program).
 */
public final class TracedThreads {

	private static final Set<IdPhantomReference> f_threads = new HashSet<IdPhantomReference>();

	static void add(final IdPhantomReference o) {
		synchronized (f_threads) {
			f_threads.add(o);
		}
	}

	static void remove(final IdPhantomReference o) {
		synchronized (f_threads) {
			f_threads.remove(o);
		}
	}

	static boolean contains(final IdPhantomReference o) {
		synchronized (f_threads) {
			return f_threads.contains(o);
		}
	}

	private TracedThreads() {
		// no instances.
	}
}
