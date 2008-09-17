package com.surelogic._flashlight;

abstract class FieldAccess extends WithinThreadEvent {

	private final ObservedField f_field;

	ObservedField getField() {
		return f_field;
	}

	FieldAccess(final ObservedField field, final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line);
		assert field != null;
		f_field = field;
	}

	protected final void addField(final StringBuilder b) {
		Entities.addAttribute("field", f_field.getId(), b);
	}

	abstract IFieldInfo getFieldInfo();
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object o);
}
