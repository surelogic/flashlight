package com.surelogic._flashlight;

abstract class FieldAccessStatic extends FieldAccess {
	FieldAccessStatic(final long field, final long siteId,
			final Store.State state, final boolean underConstruction) {
		super(field, siteId, state);
		f_classUnderConstruction = underConstruction;
	}

	private final boolean f_classUnderConstruction;

	boolean receiverUnderConstruction() {
		return f_classUnderConstruction;
	}

	@Override
	IFieldInfo getFieldInfo() {
		return ObservedField.getFieldInfo();
	}

	@Override
	public int hashCode() {
		return (int) getFieldId();
	}

	protected final void addClassUnderConstruction(final StringBuilder b) {
		if (f_classUnderConstruction) {
			Entities.addAttribute("under-construction", "yes", b);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof FieldAccessStatic) {
			final FieldAccessStatic s = (FieldAccessStatic) o;
			return this.getFieldId() == s.getFieldId();
		} else if (o instanceof SingleThreadedFieldStatic) {
			final SingleThreadedFieldStatic s = (SingleThreadedFieldStatic) o;
			return this.getFieldId() == s.getFieldId();
		}
		return false;
	}
}
