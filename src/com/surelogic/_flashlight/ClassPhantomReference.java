package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.ReferenceType;

/**
 * @region private static ClassPRInstanceMap
 * @lock ClassPRInstanceMapLock is f_classToPhantom protects ClassPRInstanceMap
 */
/* Made public so that instrumented classfiles can keep a reference to an
 * instance to avoid repeated calls to Phantom.ofClass() in SrcLoc.
 */
public final class ClassPhantomReference extends IdPhantomReference {

	/**
	 * Map from an {@link Class} to its associated {@link ClassPhantomReference}.
	 * The key of this map is not prevented from being garbage collected. Sadly,
	 * the weak identity hash map we are using is implemented for JDK 1.4,
	 * however, that makes this class easy to back port.
	 * 
	 * @unique
	 * @aggregate Instance into ClassPRInstanceMap
	 */
	private static final ConcurrentMap<Class,ClassPhantomReference> f_classToPhantom = 
		new ConcurrentReferenceHashMap<Class,ClassPhantomReference>(ReferenceType.WEAK, ReferenceType.STRONG, true);

	private static final RefFactory<Class,ClassPhantomReference> f_factory = 
		new RefFactory<Class,ClassPhantomReference>() {
			public ClassPhantomReference newReference(Class o, ReferenceQueue q) {
				return new ClassPhantomReference(o, q);
			}		
	};
	
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
		return getInstance(c, q, f_classToPhantom, f_factory);
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
