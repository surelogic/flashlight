package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.rewriter.runtime.IdObject;

abstract class IdPhantomReference extends PhantomReference {

	/**
	 * Use a thread-safe counter.
	 */
	//private static final AtomicLong f_phantomCount = new AtomicLong();

	private final long f_id = IdObject.getNewId(); //f_phantomCount.incrementAndGet();
	private boolean ignore = false;
	
	long getId() {
		return f_id;
	}

	void setToIgnore() {
		ignore = true;
	}
	
	boolean shouldBeIgnored() {
		return ignore;
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

	interface RefFactory<K,V extends IdPhantomReference> {		
		V newReference(K o, ReferenceQueue q);
	}
	
	static <K,V extends IdPhantomReference> V getInstance(final K o, final ReferenceQueue q,
			                                       final ConcurrentMap<K,V> map,
			                                       RefFactory<K,V> factory) {
		V pr                   = map.get(o);
		boolean phantomExisted = pr != null; 
		if (!phantomExisted) {
			V pr2 = factory.newReference(o, q);
			pr = map.putIfAbsent(o, pr2);			
			if (pr != null) {
				// Created an extra phantom, so kill the extra
				phantomExisted = true;
				pr2.setToIgnore();
			} else {
				/*
				System.out.println(o);
				System.out.println(map.get(o));
				*/
				pr = pr2;
			}
			/*
		} else {
		    factory = null;
		    */
		}
		/*
		 * We want to release the lock before we notify observers because, well,
		 * who knows what they will do and we wouldn't want to deadlock.
		 */
		if (!phantomExisted) {
			pr.notifyObservers();
		}
		return pr;
	}
	
	/**
	 * Accepts this phantom reference on the passed visitor.
	 * 
	 * @param v
	 *            the visitor for this phantom reference.
	 */
	abstract void accept(final IdPhantomReferenceVisitor v);
}
