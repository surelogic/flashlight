package com.surelogic._flashlight.rewriter.runtime;

import java.util.concurrent.atomic.AtomicLong;


/**
 * Instrumentation rewrites all classes that extend from
 * {@code java.lang.Object} to extends from this class instead. This class
 * provides a faster unique ID lookup than is available through
 * {@link System#identityHashCode(Object)}.
 */
public class IdObject extends Object {	
	/**
	 * Use a thread-safe counter.
	 */
	private static final AtomicLong f_idCount = new AtomicLong();

	public final long id = f_idCount.incrementAndGet();
	
	public final int identity$HashCode() {
		return (int) id;
		//return super.hashCode();
	}
	
	public static final long getNewId() {
		return f_idCount.incrementAndGet();
	}
}
