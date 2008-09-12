package com.surelogic._flashlight;

abstract class KeyField {

	final ObservedField f_field;

	KeyField(final ObservedField field) {
		assert field != null;
		f_field = field;
	}

  /**
   * Get the object that this field is a part of.
   */
	abstract IdPhantomReference getWithin();
	
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
