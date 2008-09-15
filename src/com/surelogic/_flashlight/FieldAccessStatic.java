package com.surelogic._flashlight;

abstract class FieldAccessStatic extends FieldAccess {
	FieldAccessStatic(ObservedField field, SrcLoc location) {
		super(field, location);
	}

	@Override
	IKeyField getKey() {
		return (IKeyFieldStatic) getField();
	}
}
