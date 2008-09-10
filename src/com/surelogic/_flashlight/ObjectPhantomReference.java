package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.ReferenceType;

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
	private static final ConcurrentMap<Object,ObjectPhantomReference> f_objectToPhantom = 
		new ConcurrentReferenceHashMap<Object,ObjectPhantomReference>(ReferenceType.WEAK, ReferenceType.STRONG, true);

	private static final RefFactory<Object,ObjectPhantomReference> f_factory = 
		new RefFactory<Object,ObjectPhantomReference>() {
			public ObjectPhantomReference newReference(Object o, ReferenceQueue q) {
				return new ObjectPhantomReference(o, q);
			}		
	};
	
	private final ClassPhantomReference f_type;

	ClassPhantomReference getType() {
		return f_type;
	}

	protected ObjectPhantomReference(final Object referent,
			final ReferenceQueue q) {
		super(referent, q);
		f_type = ClassPhantomReference.getInstance(referent.getClass(), q);
	}

	static ObjectPhantomReference getInstance(final Object o, final ReferenceQueue q) {
		return getInstance(o, q, f_objectToPhantom, f_factory);
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
