package com.surelogic._flashlight;

abstract class FieldAccess extends TracedEvent {

	private final long f_fieldId;

	long getFieldId() {
		return f_fieldId;
	}

	FieldAccess(final long fieldId, final long siteId,
			final PostMortemStore.State state) {
		super(siteId, state);
		f_fieldId = fieldId;
	}

	protected final void addField(final StringBuilder b) {
		Entities.addAttribute("field", f_fieldId, b);
	}

	abstract IFieldInfo getFieldInfo();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object o);
}
