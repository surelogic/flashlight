package com.surelogic._flashlight.rewriter.runtime;

import java.util.concurrent.atomic.AtomicLong;

public class IdObject {	
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
