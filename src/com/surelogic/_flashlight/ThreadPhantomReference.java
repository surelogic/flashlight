package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;

import com.surelogic._flashlight.emory.WeakIdentityHashMap;

/**
 * @region private static ThreadPRInstanceMap
 * @lock ThreadPRInstanceMapLock is f_threadToPhantom protects
 *       ThreadPRInstanceMap
 */
final class ThreadPhantomReference extends ObjectPhantomReference {

	/**
	 * Map from a {@link Thread} instance to its associated
	 * {@link ClassPhantomReference}. The key of this map is not prevented from
	 * being garbage collected. Sadly, the weak identity hash map we are using
	 * is implemented for JDK 1.4, however, that makes this class easy to back
	 * port.
	 * 
	 * @unique
	 * @aggregate Instance into ThreadPRInstanceMap
	 */
	private static final WeakIdentityHashMap f_threadToPhantom = new WeakIdentityHashMap();

	private final String f_threadName;

	/**
	 * Gets the name of this thread.
	 * 
	 * @return the name of this thread.
	 */
	String getName() {
		return f_threadName;
	}

	private ThreadPhantomReference(final Thread referent, final ReferenceQueue q) {
		super(referent, q);
		f_threadName = referent.getName();
	}

	static ThreadPhantomReference getInstance(final Thread c,
			final ReferenceQueue q) {
		ThreadPhantomReference pr;
		final boolean phantomExisted;
		synchronized (f_threadToPhantom) {
			pr = (ThreadPhantomReference) f_threadToPhantom.get(c);
			phantomExisted = pr != null;
			if (!phantomExisted) {
				pr = new ThreadPhantomReference(c, q);
				f_threadToPhantom.put(c, pr);
			}
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

	@Override
	void accept(IdPhantomReferenceVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		return "[ThreadPhantom: id=" + getId() + " name=" + getName()
				+ " type=" + getType() + "]";
	}
}
