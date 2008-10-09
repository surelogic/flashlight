package com.surelogic._flashlight.rewriter.runtime;

import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.*;


/**
 * Instrumentation rewrites all classes that extend from
 * {@code java.lang.Object} to extends from this class instead. This class
 * provides a faster unique ID lookup than is available through
 * {@link System#identityHashCode(Object)}.
 */
public class IdObject implements IIdObject {	
	/**
	 * Use a thread-safe counter.
	 * Starting from 1
	 */
	private static final AtomicLong f_idCount = new AtomicLong();
	
	public static final long getNewId() {
		return f_idCount.incrementAndGet();
	}

	private final ObjectPhantomReference phantom = Store.getObjectPhantom(this, IdObject.getNewId());
	
	public final int identity$HashCode() {
		return (int) phantom.getId();
	}
	
	public final ObjectPhantomReference getPhantom$Reference() {
		return phantom;
	}
	
	public IdObject() {}
}
