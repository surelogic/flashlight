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
public class ObjectPhantomReference extends IdPhantomReference {
	/**
	 * Map from an {@link Object} to its associated
	 * {@link ObjectPhantomReference}. The key of this map is not prevented from
	 * being garbage collected. Sadly, the weak identity hash map we are using
	 * is implemented for JDK 1.4, however, that makes this class easy to back
	 * port.
	 * 
	 * @unique
	 * @aggregate Instance into ObjectPRInstanceMap
	 */
	private static final ConcurrentMap<Object, ObjectPhantomReference> f_objectToPhantom = new ConcurrentReferenceHashMap<Object, ObjectPhantomReference>(
			ReferenceType.WEAK, ReferenceType.STRONG, hasher);

	private static final RefFactory<Object, ObjectPhantomReference> f_factory = new AbstractRefFactory<Object, ObjectPhantomReference>() {
		public ObjectPhantomReference newReference(final Object o,
				final ReferenceQueue q, final long id) {
			return new ObjectPhantomReference(o, q, id);
		}
	};

	/*
	 * private final ClassPhantomReference f_type;
	 * 
	 * ClassPhantomReference getType() { return f_type; }
	 */

	private boolean underConstruction = false;

	/**
	 * TODO may want to put it somewhere else to optimize space
	 */
	private IdPhantomReference f_thread = null;

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
			final ReferenceQueue q, final long id) {
		super(referent, q, id);
		// f_type = ClassPhantomReference.getInstance(referent.getClass(), q);
	}

	static ObjectPhantomReference getInstance(final Object o, final long id,
			final ReferenceQueue q) {
		return getInstance(o, q, id, f_objectToPhantom, f_factory);
	}

	@Override
	void accept(final ObjectDefinition defn, final IdPhantomReferenceVisitor v) {
		v.visit(defn, this);
	}

	@Override
	public String toString() {
		return "[ObjectPhantom: id=" + getId() /* + " type=" + getType() */
				+ "]";
	}

	static SingleThreadedRefs getAllSingleThreadedFields() {
		final SingleThreadedRefs refs = new SingleThreadedRefs();
		for (final ObjectPhantomReference obj : f_objectToPhantom.values()) {
			final IFieldInfo info = obj.getFieldInfo();
			info.getSingleThreadedFields(refs);
			info.clear();

			if (!obj.sharedByThreads()) {
				refs.addSingleThreadedObject(obj);
			}
		}
		return refs;
	}

	public synchronized boolean isUnderConstruction() {
		return underConstruction;
	}

	public synchronized void setUnderConstruction(final boolean constructing) {
		underConstruction = constructing;
	}

	synchronized void setLastThread(final IdPhantomReference withinThread) {
		// System.out.println("Setting last thread in OPR");
		if (f_thread == null) {
			f_thread = withinThread;
		} else if (f_thread != withinThread) {
			f_thread = IFieldInfo.SHARED_BY_THREADS;
		}
	}

	synchronized boolean sharedByThreads() {
		return f_thread == IFieldInfo.SHARED_BY_THREADS;
	}
}
