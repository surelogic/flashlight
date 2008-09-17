package com.surelogic._flashlight;

abstract class FieldAccessStatic extends FieldAccess {
	FieldAccessStatic(ObservedField field, final ClassPhantomReference withinClass, final int line) {
		super(field, withinClass, line);
	}
	
	@Override
	IFieldInfo getFieldInfo() {
		return (IFieldInfo) getField();
	}
	
	@Override
	public int hashCode() {
		return getField().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FieldAccessStatic) {
			FieldAccessStatic s = (FieldAccessStatic) o;
			return this.getField() == s.getField();
		}
		else if (o instanceof SingleThreadedFieldStatic) {
			SingleThreadedFieldStatic s = (SingleThreadedFieldStatic) o;
			return this.getField() == s.getField();
		}
		return false;
	}
}
