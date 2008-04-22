package com.surelogic._flashlight;

abstract class FieldAccess extends WithinThreadEvent {

	private final ObservedField f_field;

	ObservedField getField() {
		return f_field;
	}

	FieldAccess(final ObservedField field, final SrcLoc location) {
		super(location);
		assert field != null;
		f_field = field;
	}

	/**
	 * Gets a key representing the field this event is about. All event specific
	 * information, such as time and the thread the event occurred within, is
	 * stripped away.
	 * 
	 * @return a key representing the field this event is about.
	 */
	KeyField getKey() {
		return new KeyFieldStatic(f_field);
	}

	protected final void addField(final StringBuilder b) {
		Entities.addAttribute("field", f_field.getId(), b);
	}
}
