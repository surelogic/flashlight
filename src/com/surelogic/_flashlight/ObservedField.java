package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the set of fields that Flashlight has observed at least one event
 * about. This class maintains the invariant that if <code>f1.equals(f2)</code>,
 * then
 * <code>ObservedField.getInstance(f1) == ObservedField.getInstance(f2)</code>.
 * The class does not keep a reference to the {@link Field} object that
 * {@link #getInstance(Field)} is passed.
 * <p>
 * This class takes a static view of the field (i.e., It doesn't track the
 * receiver). For example, if a class <code>A</code> has a field
 * <code>f</code> and you create 10 instances, then the <code>f</code> field
 * within all of the instances would be the same according to this class.
 */
final class ObservedField {

	static private final AtomicLong f_observedFieldCount = new AtomicLong();

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

	private final boolean f_isFinal;

	boolean isFinal() {
		return f_isFinal;
	}

	private final boolean f_isStatic;

	boolean isStatic() {
		return f_isStatic;
	}

	private final boolean f_isVolatile;

	boolean isVolatile() {
		return f_isVolatile;
	}

	private ObservedField(final ClassPhantomReference declaringType,
			final String fieldName, final boolean isFinal,
			final boolean isStatic, final boolean isVolatile) {
		/*
		 * We create a lot of instances that we don't use to allow getInstance()
		 * to run concurrently. It is possible, however unlikely, that we could
		 * wrap f_observedFieldCount around.
		 */
		f_id = f_observedFieldCount.incrementAndGet();
		f_declaringType = declaringType;
		f_fieldName = fieldName;
		f_isFinal = isFinal;
		f_isStatic = isStatic;
		f_isVolatile = isVolatile;
	}

	/**
	 * We want to avoid locking because {@link #getInstance(Class, String)} is
	 * called from the multi-threaded portion of the {@link Store} and locking
	 * would cause blocking of the program being observed. Hence we use
	 * {@link ConcurrentHashMap}s to remember instances of this class. Only
	 * profiling will tell if this is a good approach (I hope it is and
	 * reflection is suppose to be fast according to Ernst).
	 */
	private static final ConcurrentHashMap<PhantomReference, ConcurrentHashMap<String, ObservedField>> f_declaringTypeToFieldNameToField = new ConcurrentHashMap<PhantomReference, ConcurrentHashMap<String, ObservedField>>();

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
	static ObservedField getInstance(final Field field,
			BlockingQueue<Event> rawQueue) {
		assert field != null;
		final String fieldName    = field.getName();
		final Class declaringType = field.getDeclaringClass();
		final ClassPhantomReference pDeclaringType = Phantom.ofClass(declaringType);
		ConcurrentHashMap<String, ObservedField> fieldNameToField = f_declaringTypeToFieldNameToField.get(pDeclaringType);
		if (fieldNameToField == null) {
			fieldNameToField = new ConcurrentHashMap<String, ObservedField>();
			f_declaringTypeToFieldNameToField.putIfAbsent(pDeclaringType, fieldNameToField);						
		} else {
			// Check the existing map to see if the field has already been created
			final ObservedField result = fieldNameToField.get(fieldName);
			if (result != null) {
				return result;
			}
		}
		// Need to create the field
		final int mod = field.getModifiers();
		final ObservedField result = new ObservedField(pDeclaringType,
				fieldName, Modifier.isFinal(mod), Modifier.isStatic(mod),
				Modifier.isVolatile(mod));		
		final ObservedField sResult = fieldNameToField.putIfAbsent(fieldName, result);
		if (sResult != null)
			return sResult;
		else {
			// put a field-definition event in the raw queue.
			Store.putInQueue(rawQueue, new FieldDefinition(result));
			return result;
		}
	}

	@Override
	public String toString() {
		return f_declaringType.getName() + "." + f_fieldName;
	}
}
