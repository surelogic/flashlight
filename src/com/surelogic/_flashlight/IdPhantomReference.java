package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

abstract class IdPhantomReference extends PhantomReference {

	/**
	 * Use a thread-safe counter.
	 */
	private static final AtomicLong f_phantomCount = new AtomicLong();

	private final long f_id = f_phantomCount.incrementAndGet();

	long getId() {
		return f_id;
	}

	protected IdPhantomReference(final Object referent, final ReferenceQueue q) {
		super(referent, q);
	}

	/**
	 * Use a thread-safe set to hold our observers.
	 */
	static final Set<IdPhantomReferenceCreationObserver> f_observers = new CopyOnWriteArraySet<IdPhantomReferenceCreationObserver>();

	static void addObserver(final IdPhantomReferenceCreationObserver o) {
		f_observers.add(o);
	}

	static void removeObserver(final IdPhantomReferenceCreationObserver o) {
		f_observers.remove(o);
	}

	protected void notifyObservers() {
		for (IdPhantomReferenceCreationObserver o : f_observers) {
			o.notify(this);
		}
	}

	/**
	 * Accepts this phantom reference on the passed visitor.
	 * 
	 * @param v
	 *            the visitor for this phantom reference.
	 */
	abstract void accept(final IdPhantomReferenceVisitor v);
}
