package com.surelogic._flashlight;

abstract class KeyField implements IKeyField {

	final ObservedField f_field;

	KeyField(final ObservedField field) {
		assert field != null;
		f_field = field;
	}

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
