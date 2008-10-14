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
	private static final int SEQUENCE_SHIFT = 8;
	private static final int SEQUENCE_MASK  = (1 << SEQUENCE_SHIFT) - 1;
	
	private static long getFirstIdInSequence() {
		long topBits = f_idCount.getAndIncrement();
		long id;
		if (topBits == 0) {
			id = 1; // 0 is for null
		} else {
			id = topBits << SEQUENCE_SHIFT;
		}
		return id;
	}
	
	private static class State {
		private long nextId = getFirstIdInSequence(); 

		long getNextId() {
			long id = nextId;
			if ((id & SEQUENCE_MASK) == SEQUENCE_MASK) { 
				// last id in sequence
				nextId = getFirstIdInSequence();
			} else {
				nextId++;
			}
			return id;
		}
	}
	
	private static final ThreadLocal<State> state = new ThreadLocal<State>() {
		@Override
		protected State initialValue() {
			return new State();
		}
	};
	
	public static final long getNewId() {
		//return f_idCount.incrementAndGet();
		return state.get().getNextId();
	}

	// Preallocating the id is needed for identity$HashCode
	private final ObjectPhantomReference phantom = Store.getObjectPhantom(this, IdObject.getNewId());
	
	public final int identity$HashCode() {
		return (int) phantom.getId();
	}
	
	public final ObjectPhantomReference getPhantom$Reference() {
		return phantom;
	}
	
	public IdObject() {
		// Nothing to do
	}
}

