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

  private final int modifier;

  FieldDefinition(final long id, final long declaringType, final String name, final int mod) {
    this.id = id;
    this.declaringType = declaringType;
    this.name = name;
    this.modifier = mod;
  }

  FieldDefinition(final ObservedField field) {
    assert field != null;
    id = field.getId();
    declaringType = field.getDeclaringType().getId();
    name = field.getName();
    modifier = field.getModifier();
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

  public int getModifier() {
    return modifier;
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
    Entities.addAttribute("mod", modifier, b);
    b.append("/>");
    return b.toString();
  }

}
