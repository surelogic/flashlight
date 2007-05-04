package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;

import com.surelogic._flashlight.emory.WeakIdentityHashMap;

/**
 * @region private static ClassPRInstanceMap
 * @lock ClassPRInstanceMapLock is f_classToPhantom protects ClassPRInstanceMap
 */
final class ClassPhantomReference extends IdPhantomReference {

	/**
	 * Map from an {@link Class} to its associated {@link ClassPhantomReference}.
	 * The key of this map is not prevented from being garbage collected. Sadly,
	 * the weak identity hash map we are using is implemented for JDK 1.4,
	 * however, that makes this class easy to back port.
	 * 
	 * @unique
	 * @aggregate Instance into ClassPRInstanceMap
	 */
	private static final WeakIdentityHashMap f_classToPhantom = new WeakIdentityHashMap();

	private final String f_className;

	/**
	 * Gets the name of this class.
	 * 
	 * @return the name of this class.
	 */
	String getName() {
		return f_className;
	}

	private ClassPhantomReference(final Class referent, final ReferenceQueue q) {
		super(referent, q);
		f_className = referent.getName();
	}

	static ClassPhantomReference getInstance(final Class c,
			final ReferenceQueue q) {
		ClassPhantomReference pr;
		final boolean phantomExisted;
		synchronized (f_classToPhantom) {
			pr = (ClassPhantomReference) f_classToPhantom.get(c);
			phantomExisted = pr != null;
			if (!phantomExisted) {
				pr = new ClassPhantomReference(c, q);
				f_classToPhantom.put(c, pr);
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
		return "[ClassPhantom: id=" + getId() + " name=" + getName() + "]";
	}
}
