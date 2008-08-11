package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

final class KeyFieldInstance extends KeyField {

	final ObjectPhantomReference f_enclosingInstance;

	KeyFieldInstance(final ObservedField field,
			final ObjectPhantomReference enclosingInstance) {
		super(field);
		assert enclosingInstance != null;
		f_enclosingInstance = enclosingInstance;
	}

	@Override
	boolean isWithin(final PhantomReference o) {
		return f_enclosingInstance == o;
	}
	
	@Override
	PhantomReference getWithin() {
	  return f_enclosingInstance;
	}

	@Override
	SingleThreadedField getSingleThreadedEventAbout() {
		return new SingleThreadedFieldInstance(f_field, f_enclosingInstance);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof KeyFieldInstance) {
			KeyFieldInstance fsk = (KeyFieldInstance) obj;
			return fsk.f_field == f_field
					&& fsk.f_enclosingInstance == f_enclosingInstance;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return f_field.hashCode() + f_enclosingInstance.hashCode();
	}

	@Override
	public String toString() {
		return f_field.getDeclaringType().getName() + "["
				+ f_enclosingInstance.getId() + "]."
				+ f_field.getDeclaringType();
	}
}
