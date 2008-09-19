package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.*;

/**
 * @region private static ObjectPRInstanceMap
 * @lock ObjectPRInstanceMapLock is f_objectToPhantom protects
 *       ObjectPRInstanceMap
 */
class ObjectPhantomReference extends IdPhantomReference implements IFieldInfo {
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
		new ConcurrentReferenceHashMap<Object,ObjectPhantomReference>(ReferenceType.WEAK, ReferenceType.STRONG, ComparisonType.IDENTITY_HASH);

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

	/**
	 * Mapping from fields to the thread it's used by (or SHARED_FIELD)
	 */
	private final Map<ObservedField,PhantomReference> f_fieldToThread =
		new HashMap<ObservedField,PhantomReference>();	
	
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

	public void setLastThread(ObservedField key, IdPhantomReference thread) {		
		final PhantomReference lastThread = f_fieldToThread.get(key);
		if (lastThread == SHARED_BY_THREADS) {
			return;
		}
		if (lastThread == null) {
			// First time to see this field: set to the current thread
			f_fieldToThread.put(key, thread);
		}
		else if (lastThread != thread) {
			// Set field as shared
			f_fieldToThread.put(key, SHARED_BY_THREADS);
		}
	}

	public void getSingleThreadedFields(Collection<SingleThreadedField> fields) {
		for(Map.Entry<ObservedField, PhantomReference> e : f_fieldToThread.entrySet()) {
			if (e.getValue() != SHARED_BY_THREADS) {
				fields.add(e.getKey().getSingleThreadedEventAbout(this));
			}
		}
	}
	
	static Set<SingleThreadedField> getAllSingleThreadedFields() {
		Set<SingleThreadedField> fields = new HashSet<SingleThreadedField>();
		for(ObjectPhantomReference obj : f_objectToPhantom.values()) {
			obj.getSingleThreadedFields(fields);
			obj.f_fieldToThread.clear();
		}
		return fields;
	}
}
