package com.surelogic._flashlight;

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;

import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap;
import com.surelogic._flashlight.jsr166y.ConcurrentReferenceHashMap.ReferenceType;

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
	private static final ConcurrentMap<Thread,ThreadPhantomReference> f_threadToPhantom = 
		new ConcurrentReferenceHashMap<Thread,ThreadPhantomReference>(ReferenceType.WEAK, ReferenceType.STRONG, true);

	private static final RefFactory<Thread,ThreadPhantomReference> f_factory = 
		new RefFactory<Thread,ThreadPhantomReference>() {
			public ThreadPhantomReference newReference(Thread o, ReferenceQueue q) {
				return new ThreadPhantomReference(o, q);
			}		
	};

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
		return getInstance(c, q, f_threadToPhantom, f_factory);
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
