package com.surelogic._flashlight;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks objects in the instrumented program that are under construction.
 */
public final class UnderConstruction {

	private static final Set<IdPhantomReference> f_object = new HashSet<IdPhantomReference>();

	static void add(final IdPhantomReference o) {
		synchronized (f_object) {
			f_object.add(o);
		}
	}

	static void remove(final IdPhantomReference o) {
		synchronized (f_object) {
			f_object.remove(o);
		}
	}

	static boolean contains(final IdPhantomReference o) {
		synchronized (f_object) {
			return f_object.contains(o);
		}
	}
}
