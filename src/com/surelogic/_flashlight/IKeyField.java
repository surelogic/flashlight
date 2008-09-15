package com.surelogic._flashlight;

public interface IKeyField {
	/**
	 * Get the object that this field is a part of.
	 */
	IdPhantomReference getWithin();

	/**
	 * Factory to create an appropriate {@link SingleThreadedField} event about
	 * this field.
	 * 
	 * @return a new {@link SingleThreadedField} event.
	 */
	SingleThreadedField getSingleThreadedEventAbout();
}
