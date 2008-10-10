package com.surelogic._flashlight;

abstract class FieldAccessStatic extends FieldAccess {
	FieldAccessStatic(long field, final long siteId, Store.State state) {
		super(field, siteId, state);
	}
	
	@Override
	IFieldInfo getFieldInfo() {
		return ObservedField.getFieldInfo();
	}
	
	@Override
	public int hashCode() {
		return (int) getFieldId();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FieldAccessStatic) {
			FieldAccessStatic s = (FieldAccessStatic) o;
			return this.getFieldId() == s.getFieldId();
		}
		else if (o instanceof SingleThreadedFieldStatic) {
			SingleThreadedFieldStatic s = (SingleThreadedFieldStatic) o;
			return this.getFieldId() == s.getFieldId();
		}
		return false;
	}
}
