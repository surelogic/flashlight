package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
//import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.rewriter.runtime.IdObject;

abstract class IdPhantomReference extends PhantomReference {
	static final ConcurrentReferenceHashMap.Hasher hasher = false ? ConcurrentReferenceHashMap.IDENTITY_HASH :
		new ConcurrentReferenceHashMap.Hasher() {
		//private int total = 0, id = 0;
		
		public int hashCode(Object o) {
			//total++;
			if (o instanceof IdObject) {
				/*
				id++;
				if ((total & 0xffff) == 0) {
					System.out.println(id+" IdObjects of "+total);
				}
				*/
				return ((IdObject) o).identity$HashCode();
			} else {
				return System.identityHashCode(o);
			}
		}
		public boolean useReferenceEquality() {
			return true;
		}
	};
	
	/**
	 * Use a thread-safe counter.
	 */
	//private static final AtomicLong f_phantomCount = new AtomicLong();

	private final long f_id = IdObject.getNewId(); //f_phantomCount.incrementAndGet();
	private boolean ignore = false;
	
	public long getId() {
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

	static List<IdPhantomReference> unnotified = new ArrayList<IdPhantomReference>();
	
	static void addObserver(final IdPhantomReferenceCreationObserver o) {		
		f_observers.add(o);
		List<IdPhantomReference> refs = null;
		synchronized (IdPhantomReference.class) {
			if (unnotified != null) {
				refs = unnotified;
				unnotified = null;
			}
		}
		if (refs != null) {
			for(IdPhantomReference ref : refs) {
				ref.notifyObservers();
			}
		}		
	}

	static void removeObserver(final IdPhantomReferenceCreationObserver o) {
		f_observers.remove(o);
	}

	protected void notifyObservers() {
		if (f_observers.isEmpty()) {
			new Throwable("No observers for IdPhantomReference").printStackTrace();
			synchronized (IdPhantomReference.class) {
				if (unnotified != null) {
					unnotified.add(this);
				}
				return;
			}
		}		
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
