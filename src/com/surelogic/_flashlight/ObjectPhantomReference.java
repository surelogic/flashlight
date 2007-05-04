package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;

import com.surelogic._flashlight.emory.WeakIdentityHashMap;

/**
 * @region private static ObjectPRInstanceMap
 * @lock ObjectPRInstanceMapLock is f_objectToPhantom protects
 *       ObjectPRInstanceMap
 */
class ObjectPhantomReference extends IdPhantomReference {

	/**
	 * Map from an {@link Object} to its associated
	 * {@link ObjectPhantomReference}. The key of this map is not prevented
	 * from being garbage collected. Sadly, the weak identity hash map we are
	 * using is implemented for JDK 1.4, however, that makes this class easy to
	 * back port.
	 * 
	 * @unique
	 * @aggregate Instance into ObjectPRInstanceMap
	 */
	private static final WeakIdentityHashMap f_objectToPhantom = new WeakIdentityHashMap();

	private final ClassPhantomReference f_type;

	ClassPhantomReference getType() {
		return f_type;
	}

	protected ObjectPhantomReference(final Object referent,
			final ReferenceQueue q) {
		super(referent, q);
		f_type = ClassPhantomReference.getInstance(referent.getClass(), q);
	}

	static ObjectPhantomReference getInstance(final Object o,
			final ReferenceQueue q) {
		ObjectPhantomReference pr;
		final boolean phantomExisted;
		synchronized (f_objectToPhantom) {
			pr = (ObjectPhantomReference) f_objectToPhantom.get(o);
			phantomExisted = pr != null;
			if (!phantomExisted) {
				pr = new ObjectPhantomReference(o, q);
				f_objectToPhantom.put(o, pr);
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
		return "[ObjectPhantom: id=" + getId() + " type=" + getType() + "]";
	}
}
