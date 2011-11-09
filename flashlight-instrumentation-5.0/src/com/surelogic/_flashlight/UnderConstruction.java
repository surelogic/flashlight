package com.surelogic._flashlight;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks objects in the instrumented program that are under construction.
 */
public final class UnderConstruction {

	private static final Map<IdPhantomReference,Object> f_object = new ConcurrentHashMap<IdPhantomReference,Object>();

	static void add(final IdPhantomReference o) {
		f_object.put(o, o);
	}

	static void remove(final IdPhantomReference o) {
		f_object.remove(o);
	}

	static boolean contains(final IdPhantomReference o) {
		return f_object.containsKey(o);
	}

	private UnderConstruction() {
		// no instances
	}
}
