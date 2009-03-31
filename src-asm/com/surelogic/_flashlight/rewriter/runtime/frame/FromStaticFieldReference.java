package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromStaticFieldReference extends SourcedStackItem {
  private final OwnedName field;
  
  /* Only create via the frame itself */
  FromStaticFieldReference(final int sloc, final OwnedName f) {
    super(sloc);
    field = f;
  }
  
  public Type getType() {
    return Type.STATIC_FIELD_REFERENCE;
  }
  
  public OwnedName getField() {
    return field;
  }
  
  @Override
  public String toString() {
    return field.owner + "." + field.name + "@" + sourceLineOfCode;
  }
}
