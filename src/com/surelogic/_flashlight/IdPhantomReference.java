package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
//import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.rewriter.runtime.*;

abstract class IdPhantomReference extends PhantomReference {
	private static final boolean useIdObject = true; 	
	/*
	private static int total = 0, idLookups = 0;
	private static int notJavaSomething = 0;
	*/
	static final ConcurrentReferenceHashMap.Hasher hasher = false ? ConcurrentReferenceHashMap.IDENTITY_HASH :
		new ConcurrentReferenceHashMap.Hasher() {
		//private int total = 0, id = 0;
		
		public int hashCode(Object o) {
			//total++;
			if (useIdObject && o instanceof IIdObject) {
				/*
				id++;
				if ((total & 0xffff) == 0) {
					System.out.println(id+" IdObjects of "+total);
				}
				*/
				return ((IIdObject) o).identity$HashCode();
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

	private final long f_id;// = IdObject.getNewId(); //f_phantomCount.incrementAndGet();
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
		this(referent, q, IdObject.getNewId());
	}
	
	protected IdPhantomReference(final Object referent, final ReferenceQueue q, long id) {
		super(referent, q);		
		f_id = id == Phantom.NO_PREASSIGNED_ID ? IdObject.getNewId() : id;		
	}

	/**
	 * Use a thread-safe set to hold our observers.
	 */
	static final Set<IdPhantomReferenceCreationObserver> f_observers = new CopyOnWriteArraySet<IdPhantomReferenceCreationObserver>();	
	
	static class Unnotified {
		final ClassPhantomReference type;
		final IdPhantomReference ref;
		
		Unnotified(ClassPhantomReference t, IdPhantomReference r) {
			type = t;
			ref = r;
		}
	}
	
	static List<Unnotified> unnotified = new ArrayList<Unnotified>();
	
	static void addObserver(final IdPhantomReferenceCreationObserver o) {		
		f_observers.add(o);
		List<Unnotified> refs = null;
		synchronized (IdPhantomReference.class) {
			if (unnotified != null) {
				refs = unnotified;
				unnotified = null;
			}
		}
		if (refs != null) {
			for(Unnotified u : refs) {
				u.ref.notifyObservers(u.type);
			}
		}		
	}

	static void removeObserver(final IdPhantomReferenceCreationObserver o) {
		f_observers.remove(o);
	}

	protected void notifyObservers(ClassPhantomReference type) {
		if (f_observers.isEmpty()) {
			//new Throwable("No observers for IdPhantomReference").printStackTrace();
			synchronized (IdPhantomReference.class) {
				if (unnotified != null) {
					unnotified.add(new Unnotified(type, this));
				}
				return;
			}
		}		
		for (IdPhantomReferenceCreationObserver o : f_observers) {
			o.notify(type, this);
		}
	}

	interface RefFactory<K,V extends IdPhantomReference> {		
		V newReference(K o, ReferenceQueue q, long id);
		V removeSpecialId(final K o);
		void recordSpecialId(final K o, final V pr);
	}
	
	static class RefNode<K,V extends IdPhantomReference> {
		final K obj;
		final V phantom;		
		RefNode<K,V> next;
		
		RefNode(K o, V pr, RefNode<K,V> next) {
			obj = o;
			phantom = pr;
			this.next = next;
		}
	}
	
	static abstract class AbstractRefFactory<K,V extends IdPhantomReference> 
	implements RefFactory<K,V> {
		RefNode<K,V> root;
		
		public synchronized final V removeSpecialId(final K o) {
			RefNode<K,V> last = null;
			RefNode<K,V> here = root;
			while (here != null) {
				if (here.obj == o) {
					// Remove entry
					if (last == null) {
						root = here.next;
					} else {
						last.next = here.next;
					}
					return here.phantom;
				}
				last = here;
				here = here.next;
			}			
			return null;
		}
		public synchronized final void recordSpecialId(final K o, final V pr) {
			RefNode<K,V> n = new RefNode<K,V>(o, pr, root);
			root = n;
		}
	}
	
	
	
	static <K,V extends IdPhantomReference> V getInstance(final K o, final ReferenceQueue q,
			                                       final long id, 
			                                       final ConcurrentMap<K,V> map,
			                                       RefFactory<K,V> factory) {
		boolean phantomExisted;
        V pr;		
		if (id != Phantom.NO_PREASSIGNED_ID) {
			//total++;
			pr = factory.removeSpecialId(o);
			if (pr != null) {
				// Already allocated
				//specialIds++;
				//System.err.println(specialIds+" special Ids out of "+total);				
				return pr;
			}
			// Must be new
			phantomExisted = false;
			pr = factory.newReference(o, q, id);

		} else {
			//total++;
			if (useIdObject && o instanceof IIdObject) {
				IIdObject ido = (IIdObject) o;
				pr = (V) ido.getPhantom$Reference();
				/*
				idLookups++;
				if ((total & 0xffff) == 0) {
					System.err.println(idLookups+" IdObject lookups of "+total);
				}
                */
				if (pr != null) {
					return pr;
				}
				// If it gets here, it's because a superclass called an overridden method
				// and the code below will allocate a phantom reference
				pr = factory.newReference(o, q, id);
				factory.recordSpecialId(o, pr);
				notifyOnCreation(o, pr, q);
				return pr;
			} else {
				pr = map.get(o);
			}
			phantomExisted = pr != null; 
			if (!phantomExisted) {
				V pr2 = factory.newReference(o, q, id);
				pr = map.putIfAbsent(o, pr2);			
				if (pr != null) {
					// Created an extra phantom, so kill the extra
					phantomExisted = true;
					pr2.setToIgnore();
				} else {
					/*
					total++;
					if (!o.getClass().getPackage().getName().startsWith("java")) {
						notJavaSomething++;
					}
					//System.err.println("Not IdObject: "+o.getClass().getName());
					if ((total & 0xff) == 0) {
						System.err.println(notJavaSomething+" non-java of "+total);
					}
					*/
					pr = pr2;
				}
				/*
		    } else {
		    factory = null;
				 */
			}
		}
		/*
		 * We want to release the lock before we notify observers because, well,
		 * who knows what they will do and we wouldn't want to deadlock.
		 */
		if (!phantomExisted) {
			notifyOnCreation(o, pr, q);
		}
		return pr;
	}

	private static <K, V extends IdPhantomReference> 
	void notifyOnCreation(final K o, V pr, final ReferenceQueue q) {
		final ClassPhantomReference type = o instanceof Class ? null : ClassPhantomReference.getInstance(o.getClass(), q);
		pr.notifyObservers(type);
	}
	
	/**
	 * Accepts this phantom reference on the passed visitor.
	 * 
	 * @param v
	 *            the visitor for this phantom reference.
	 */
	abstract void accept(final ObjectDefinition defn, final IdPhantomReferenceVisitor v);
}
