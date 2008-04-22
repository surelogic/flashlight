package com.surelogic._flashlight;

/**
 * This event defines an identifier for a field so that all other events can
 * reference fields by their identifiers. This event always occurs in the output
 * before any other event about the field it defines.
 */
final class FieldDefinition extends DefinitionEvent {

	private final ObservedField f_field;

	ObservedField getField() {
		return f_field;
	}

	FieldDefinition(final ObservedField field) {
		assert field != null;
		f_field = field;
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<field-definition");
		Entities.addAttribute("id", f_field.getId(), b);
		Entities.addAttribute("type", f_field.getDeclaringType().getId(), b);
		Entities.addAttribute("field", f_field.getName(), b);
		if (f_field.isStatic())
			Entities.addAttribute("static", "yes", b);
		if (f_field.isFinal())
			Entities.addAttribute("final", "yes", b);
		if (f_field.isVolatile())
			Entities.addAttribute("volatile", "yes", b);
		b.append("/>");
		return b.toString();
	}
}
