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
// Made public so that instances can be held by the instrumented classfiles
public final class Phantom {
	static final long NO_PREASSIGNED_ID = -1;
	
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
   * <p>
   * This method is {@code public} so that it may be called from the
   * {@code getClassPhantom()} method of the {@link EmptyStore} and
   * {@link DebugStore} classes. This method should not be called directly
   * outside of the Store. Use {@code getClassPhantom(Class)} on the approciate
   * store class instead.
   * 
   * @param c
   *          the non-null class.
   * @return the class's phantom reference.
   */
	public static ClassPhantomReference ofClass(final Class c) {
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
		return ofObject(o, NO_PREASSIGNED_ID);
	}

	static ObjectPhantomReference ofObject(final Object o, long id) {
		assert o != null;
		if (o instanceof Class)
			throw new IllegalArgumentException(
					"the object cannot be an instance of Class");
		if (o instanceof Thread)
			return ofThread((Thread) o, id);
		else
			return ObjectPhantomReference.getInstance(o, id, f_collected);
	}
	
	/**
	 * Gets the associated thread phantom reference for the passed thread.
	 * 
	 * @param t
	 *            the non-null thread.
	 * @return the thread's phantom reference.
	 */
	public static ThreadPhantomReference ofThread(final Thread t) {
		assert t != null;
		return ThreadPhantomReference.getInstance(t, f_collected, NO_PREASSIGNED_ID);
	}
	
	static ThreadPhantomReference ofThread(final Thread t, long id) {
		assert t != null;
		return ThreadPhantomReference.getInstance(t, f_collected, id);
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

	static IdPhantomReference get() {
		try {
			return (IdPhantomReference) f_collected.remove();
		} catch (InterruptedException e) {
			return null;
		}		
	}
	
	private Phantom() {
		// no instances
	}
}
