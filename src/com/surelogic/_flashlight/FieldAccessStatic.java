package com.surelogic._flashlight;

abstract class FieldAccessStatic extends FieldAccess {
	FieldAccessStatic(ObservedField field, final ClassPhantomReference withinClass, final int line) {
		super(field, withinClass, line);
	}

	@Override
	IKeyField getKey() {
		return (IKeyFieldStatic) getField();
	}
}
