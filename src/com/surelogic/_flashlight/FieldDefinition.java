package com.surelogic._flashlight;

/**
 * This event defines an identifier for a field so that all other events can
 * reference fields by their identifiers. This event always occurs in the output
 * before any other event about the field it defines.
 */
final class FieldDefinition extends DefinitionalEvent {
	private final long id;
	private final long declaringType;
	private final String name;
	private final boolean isStatic, isFinal, isVolatile;

	FieldDefinition(long id, long declaringType, String name, 
			        boolean isStatic, boolean isFinal, boolean isVolatile) {
		this.id = id;
		this.declaringType = declaringType;
		this.name = name;
		this.isStatic = isStatic;
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
	}
	
	FieldDefinition(final ObservedField field) {
		assert field != null;
		id = field.getId();
		declaringType = field.getDeclaringType().getId();
		name = field.getName();
		isStatic = field.isStatic();
		isFinal = field.isFinal();
		isVolatile = field.isVolatile();
	}

	long getId() {
		return id;
	}
	
	long getTypeId() {
		return declaringType;
	}
	
	String getName() {
		return name;
	}
	
	boolean isStatic() {
		return isStatic;
	}
	
	boolean isFinal() {
		return isFinal;
	}
	
	boolean isVolatile() {
		return isVolatile;
	}
	
	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<field-definition");
		Entities.addAttribute("id", id, b);
		Entities.addAttribute("type", declaringType, b);
		Entities.addAttribute("field", name, b);
		if (isStatic)
			Entities.addAttribute("static", "yes", b);
		if (isFinal)
			Entities.addAttribute("final", "yes", b);
		if (isVolatile)
			Entities.addAttribute("volatile", "yes", b);
		b.append("/>");
		return b.toString();
	}
}
