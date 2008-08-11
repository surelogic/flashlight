package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

final class KeyFieldStatic extends KeyField {

	KeyFieldStatic(final ObservedField field) {
		super(field);
	}

	@Override
	boolean isWithin(final PhantomReference o) {
		return f_field.getDeclaringType() == o;
	}

	@Override
	PhantomReference getWithin() {
	  return f_field.getDeclaringType();
	}
	
	@Override
	SingleThreadedField getSingleThreadedEventAbout() {
		return new SingleThreadedFieldStatic(f_field);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof KeyFieldStatic) {
			KeyFieldStatic fsk = (KeyFieldStatic) obj;
			return fsk.f_field == f_field;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return f_field.hashCode();
	}

	@Override
	public String toString() {
		return f_field.getDeclaringType().getName() + "." + f_field.getName();
	}
}
