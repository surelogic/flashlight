package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collection;

/**
 * Maintains a mapping from objects within the program to associated
 * {@link PhantomReference} objects and communicates what objects have been
 * garbage collected. This class maintains the invariant that if
 * <code>o1 == o2</code>, then <code>Phantom.of(o1) == Phantom.of(o2)</code>.
 * It also is able to provide a list of what objects have been garbage
 * collected.
 */
// Made public so that ofClass() can be called from instrumented classfiles
public final class Phantom {

	private static final ReferenceQueue f_collected = new ReferenceQueue();

	/**
	 * Gets the phantom reference for the passed object.
	 * 
	 * @param o
	 *            the non-null object.
	 * @return the object's phantom reference.
	 */
	static IdPhantomReference of(final Object o) {
		if (o instanceof Class)
			return ofClass((Class) o);
		else if (o instanceof Thread)
			return ofThread((Thread) o);
		else
			return ofObject(o);
	}

	/**
	 * Gets the associated class phantom reference for the passed class.
	 * 
	 * @param c
	 *            the non-null class.
	 * @return the class's phantom reference.
	 */
	static ClassPhantomReference ofClass(final Class c) {
		assert c != null;
		return ClassPhantomReference.getInstance(c, f_collected);
	}

	/**
	 * Gets the associated object phantom reference for the passed object.
	 * 
	 * @param o
	 *            the non-null object (cannot be an instance of {@link Class}).
	 * @return the object's phantom reference.
	 * @throws IllegalArgumentException
	 *             if the predicate <code>(o instanceof Class)</code> is true.
	 */
	static ObjectPhantomReference ofObject(final Object o) {
		assert o != null;
		if (o instanceof Class)
			throw new IllegalArgumentException(
					"the object cannot be an instance of Class");
		if (o instanceof Thread)
			return ofThread((Thread) o);
		else
			return ObjectPhantomReference.getInstance(o, f_collected);
	}

	/**
	 * Gets the associated thread phantom reference for the passed thread.
	 * 
	 * @param t
	 *            the non-null thread.
	 * @return the thread's phantom reference.
	 */
	static ThreadPhantomReference ofThread(final Thread t) {
		assert t != null;
		return ThreadPhantomReference.getInstance(t, f_collected);
	}

	/**
	 * Removes all available elements from the queue of garbage collected
	 * objects and adds them, as {@link IdPhantomReference}s, into the given
	 * collection. The behavior of this operation is undefined if the specified
	 * collection is modified while the operation is in progress.
	 * 
	 * @param c
	 *            the collection to transfer elements into.
	 * @return the number of elements transferred.
	 */
	static int drainTo(final Collection<IdPhantomReference> c) {
		int count = 0;
		while (true) {
			IdPhantomReference pr = (IdPhantomReference) f_collected.poll();
			if (pr == null)
				return count;
			c.add(pr);
			count++;
		}
	}

	private Phantom() {
		// no instances
	}
}
