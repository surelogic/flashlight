package com.surelogic._flashlight;

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
public class ObjectPhantomReference extends IdPhantomReference {
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
		new ConcurrentReferenceHashMap<Object,ObjectPhantomReference>(ReferenceType.WEAK, ReferenceType.STRONG, hasher);

	private static final RefFactory<Object,ObjectPhantomReference> f_factory = 
		new RefFactory<Object,ObjectPhantomReference>() {
			public ObjectPhantomReference newReference(Object o, ReferenceQueue q, long id) {
				return new ObjectPhantomReference(o, q, id);
			}		
	};
	
	/*
	private final ClassPhantomReference f_type;

	ClassPhantomReference getType() {
		return f_type;
	}
	*/

	/**
	 * Mapping from fields to the thread it's used by (or SHARED_FIELD)
	 */
	private IFieldInfo f_info = null;
	
	private IFieldInfo makeFieldInfo() {
		return new ObservedField.FieldInfo() {	
			@Override
			public ObjectPhantomReference getReceiver() { 
				return ObjectPhantomReference.this;
			}
		};
	}
	
	public IFieldInfo getFieldInfo() {
		if (f_info == null) {
			f_info = makeFieldInfo();
		}
		return f_info;
	}
	
	protected ObjectPhantomReference(final Object referent,
			final ReferenceQueue q, long id) {
		super(referent, q, id);
		//f_type = ClassPhantomReference.getInstance(referent.getClass(), q);
	}

	static ObjectPhantomReference getInstance(final Object o, final long id, final ReferenceQueue q) {
		return getInstance(o, q, id, f_objectToPhantom, f_factory);
	}

	@Override
	void accept(final ObjectDefinition defn, IdPhantomReferenceVisitor v) {
		v.visit(defn, this);
	}

	@Override
	public String toString() {
		return "[ObjectPhantom: id=" + getId() /*+ " type=" + getType()*/ + "]";
	}
	
	static Set<SingleThreadedField> getAllSingleThreadedFields() {
		Set<SingleThreadedField> fields = new HashSet<SingleThreadedField>();
		for(ObjectPhantomReference obj : f_objectToPhantom.values()) {
			IFieldInfo info = obj.getFieldInfo();
			info.getSingleThreadedFields(fields);
			info.clear();
		}
		return fields;
	}
}
