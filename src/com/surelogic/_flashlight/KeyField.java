package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

abstract class KeyField {

	final ObservedField f_field;

	KeyField(final ObservedField field) {
		assert field != null;
		f_field = field;
	}

	/**
	 * Determines if this field is within the passed object.
	 * 
	 * @param o
	 *            the phantom of the object.
	 * @return <code>true</code> if the this field is within the passed
	 *         object, <code>false</code> otherwise.
	 */
	abstract boolean isWithin(final PhantomReference o);

	abstract PhantomReference getWithin();
	
	/**
	 * Factory to create an appropriate {@link SingleThreadedField} event about
	 * this field.
	 * 
	 * @return a new {@link SingleThreadedField} event.
	 */
	abstract SingleThreadedField getSingleThreadedEventAbout();

	@Override
	public boolean equals(Object obj) {
		throw new IllegalStateException("subclasses must override");
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException("subclasses must override");
	}

	@Override
	public String toString() {
		throw new IllegalStateException("subclasses must override");
	}
}
