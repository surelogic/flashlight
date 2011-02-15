package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.common.LongMap;

/**
 * Manages the set of fields that Flashlight has observed at least one event
 * about. This class maintains the invariant that if <code>f1.equals(f2)</code>,
 * then
 * <code>ObservedField.getInstance(f1) == ObservedField.getInstance(f2)</code>.
 * The class does not keep a reference to the {@link Field} object that
 * {@link #getInstance(Field)} is passed.
 * <p>
 * This class takes a static view of the field (i.e., It doesn't track the
 * receiver). For example, if a class <code>A</code> has a field <code>f</code>
 * and you create 10 instances, then the <code>f</code> field within all of the
 * instances would be the same according to this class.
 */
abstract class ObservedField {

	static private final AtomicLong f_observedFieldCount = new AtomicLong(-1L);

	private final long f_id;

	long getId() {
		return f_id;
	}

	private final ClassPhantomReference f_declaringType;

	ClassPhantomReference getDeclaringType() {
		return f_declaringType;
	}

	private final String f_fieldName;

	String getName() {
		return f_fieldName;
	}

	private final int f_modifier;

	int getModifier() {
		return f_modifier;
	}

	static final SingleThreadedField getSingleThreadedEventAbout(
			final long field, final ObjectPhantomReference receiver) {
		if (receiver != null) { // FIX
			return new SingleThreadedFieldInstance(field, receiver);
		} else {
			return new SingleThreadedFieldStatic(field);
		}
	}

	private ObservedField(final ClassPhantomReference declaringType,
			final String fieldName, final int modifier) {
		/*
		 * We create a lot of instances that we don't use to allow getInstance()
		 * to run concurrently. It is possible, however unlikely, that we could
		 * wrap f_observedFieldCount around.
		 */
		f_id = f_observedFieldCount.getAndDecrement();
		f_declaringType = declaringType;
		f_fieldName = fieldName;
		f_modifier = modifier;
	}

	/**
	 * We want to avoid locking because {@link #getInstance(Class, String)} is
	 * called from the multi-threaded portion of the {@link Store} and locking
	 * would cause blocking of the program being observed. Hence we use
	 * {@link ConcurrentHashMap}s to remember instances of this class. Only
	 * profiling will tell if this is a good approach (I hope it is and
	 * reflection is suppose to be fast according to Ernst).
	 */
	private static final ConcurrentHashMap<ClassPhantomReference, ConcurrentHashMap<String, ObservedField>> f_declaringTypeToFieldNameToField = new ConcurrentHashMap<ClassPhantomReference, ConcurrentHashMap<String, ObservedField>>();

	/**
	 * Returns the one {@link ObservedField} instance associated with the
	 * specified field. This method places a {@link FieldDefinition} event into
	 * the specified queue if the field has not been observed previously.
	 * 
	 * @param field
	 *            a field within the instrumented program.
	 * @param rawQueue
	 *            the queue to place a {@link FieldDefinition} event into if
	 *            this field has not been observed previously.
	 * @return the one {@link ObservedField} instance associated with the
	 *         specified field.
	 */
	static ObservedField getInstance(final String className,
			final String fieldName, final PostMortemStore.State state) {
		assert className != null && fieldName != null;
		final Class declaringType;
		try {
			declaringType = Class.forName(className);
		} catch (final ClassNotFoundException e) {
			return null;
		}
		return getInstance(declaringType, fieldName, state);
	}

	static ObservedField getInstance(final Field field,
			final PostMortemStore.State state) {
		return getInstance(field.getDeclaringClass(), field.getName(), state);
	}

	static ObservedField getInstance(final Class declaringType,
			final String fieldName, final PostMortemStore.State state) {
		final ClassPhantomReference pDeclaringType = Phantom
				.ofClass(declaringType);
		ConcurrentHashMap<String, ObservedField> fieldNameToField = f_declaringTypeToFieldNameToField
				.get(pDeclaringType);
		if (fieldNameToField == null) {
			final ConcurrentHashMap<String, ObservedField> temp = new ConcurrentHashMap<String, ObservedField>();
			fieldNameToField = f_declaringTypeToFieldNameToField.putIfAbsent(
					pDeclaringType, temp);
			if (fieldNameToField == null) {
				fieldNameToField = temp;
			}
		} else {
			// Check the existing map to see if the field has already been
			// created
			final ObservedField result = fieldNameToField.get(fieldName);
			if (result != null) {
				return result;
			}
		}
		// Need to create the field
		final Field field;
		try {
			field = getFieldInternal(declaringType, fieldName);
		} catch (final NoSuchFieldException e) {
			return null;
		}
		final int mod = field.getModifiers();
		final ObservedField result = Modifier.isStatic(mod) ? new Static(
				pDeclaringType, fieldName, mod) : new Instance(pDeclaringType,
				fieldName, mod);
		final ObservedField sResult = fieldNameToField.putIfAbsent(fieldName,
				result);
		if (sResult != null) {
			return sResult;
		} else {
			// put a field-definition event in the raw queue.
			PostMortemStore.putInQueue(state, new FieldDefinition(result));
			return result;
		}
	}

	/**
	 * Originally in FlashlightRuntimeSupport
	 */
	private static Field getFieldInternal(final Class root, final String fname)
			throws NoSuchFieldException {
		try {
			/* Hopefully the field is local. */
			final Field f = root.getDeclaredField(fname);
			// Won't get here if the field is not found
			return f;
		} catch (final NoSuchFieldException e) {
			// Fall through to try super class and interfaces
		}

		final Class superClass = root.getSuperclass();
		if (superClass != null) {
			try {
				return getFieldInternal(superClass, fname);
			} catch (final NoSuchFieldException e) {
				// fall through to check interfaces
			}
		}

		final Class[] interfaces = root.getInterfaces();
		for (final Class i : interfaces) {
			try {
				return getFieldInternal(i, fname);
			} catch (final NoSuchFieldException e) {
				// try next interface
			}
		}

		// Couldn't find the field
		throw new NoSuchFieldException("Couldn't find field \"" + fname
				+ "\" in class " + root.getCanonicalName()
				+ " or any of its ancestors");
	}

	@Override
	public String toString() {
		return f_declaringType.getName() + "." + f_fieldName;
	}

	private static class Instance extends ObservedField {
		public Instance(final ClassPhantomReference declaringType,
				final String fieldName, final int modifier) {
			super(declaringType, fieldName, modifier);
		}

	}

	private static class Static extends ObservedField {
		public Static(final ClassPhantomReference declaringType,
				final String fieldName, final int modifier) {
			super(declaringType, fieldName, modifier);
		}

	}

	public static IFieldInfo getFieldInfo() {
		return staticInfo;
	}

	private static final IFieldInfo staticInfo = new FieldInfo();

	// private static int numInfos = 0, numFields = 0;

	/**
	 * Mapping from fields to the thread it's used by (or SHARED_BY_THREADS)
	 */
	static class FieldInfo extends LongMap<IdPhantomReference> implements
			IFieldInfo {
		FieldInfo() {
			super(2);
			// numInfos++;
		}

		static final IdPhantomReference SHARED_BY_THREADS = Phantom
				.ofClass(FieldInfo.class);

		public void setLastThread(final long key,
				final IdPhantomReference thread) {
			final PhantomReference lastThread = this.get(key);
			if (lastThread == SHARED_BY_THREADS) {
				return;
			}
			if (lastThread == null) {
				// First time to see this field: set to the current thread
				this.put(key, thread);
				/*
				 * numFields++; if ((numFields & 0xfff) == 0) {
				 * System.err.println(numFields+" in "+numInfos); }
				 */
			} else if (lastThread != thread) {
				// Set field as shared
				this.put(key, SHARED_BY_THREADS);
			}
		}

		/**
		 * @return true if adding something
		 */
		public boolean getSingleThreadedFields(final SingleThreadedRefs refs) {
			boolean added = false;
			/*
			 * Iterator<Map.Entry<Long, IdPhantomReference>> it =
			 * this.entrySet().iterator(); while (it.hasNext()) {
			 * Map.Entry<Long, IdPhantomReference> e = it.next();
			 */
			for (final Map.Entry<Long, IdPhantomReference> me : this.entrySet()) {
				final LongMap.Entry<IdPhantomReference> e = (LongMap.Entry<IdPhantomReference>) me;
				if (e.getValue() != SHARED_BY_THREADS) {
					added = true;
					refs.addField(ObservedField.getSingleThreadedEventAbout(
							e.key(), getReceiver()));
				}
			}
			return added;
		}

		public ObjectPhantomReference getReceiver() {
			return null;
		}
	}

}
