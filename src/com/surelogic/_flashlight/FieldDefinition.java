package com.surelogic._flashlight;

import java.lang.reflect.Modifier;

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
	private final int visibility;

	FieldDefinition(final long id, final long declaringType, final String name,
			final boolean isStatic, final boolean isFinal,
			final boolean isVolatile, final int viz) {
		this.id = id;
		this.declaringType = declaringType;
		this.name = name;
		this.isStatic = isStatic;
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
		this.visibility = viz;
	}

	FieldDefinition(final ObservedField field) {
		assert field != null;
		id = field.getId();
		declaringType = field.getDeclaringType().getId();
		name = field.getName();
		isStatic = field.isStatic();
		isFinal = field.isFinal();
		isVolatile = field.isVolatile();
		visibility = field.getVisibility();
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

	public int getVisibility() {
		return visibility;
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<field-definition");
		Entities.addAttribute("id", id, b);
		Entities.addAttribute("type", declaringType, b);
		Entities.addAttribute("field", name, b);
		Entities.addAttribute("visibility", visibility, b);
		if (isStatic) {
			Entities.addAttribute("static", "yes", b);
		}
		if (isFinal) {
			Entities.addAttribute("final", "yes", b);
		}
		if (isVolatile) {
			Entities.addAttribute("volatile", "yes", b);
		}
		b.append("/>");
		return b.toString();
	}

	/**
	 * Return the 'visibility' value of a field from the modifier.
	 * 
	 * @param mod
	 * @return
	 */
	public static int fromModifier(final int mod) {
		if (Modifier.isPrivate(mod)) {
			return Modifier.PRIVATE;
		} else if (Modifier.isProtected(mod)) {
			return Modifier.PROTECTED;
		} else if (Modifier.isPublic(mod)) {
			return Modifier.PUBLIC;
		} else {
			return 0;
		}
	}

}
