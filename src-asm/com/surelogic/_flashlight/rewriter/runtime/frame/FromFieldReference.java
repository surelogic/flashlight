package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromFieldReference extends SourcedStackItem {
  private final StackItem object;
  private final OwnedName field;
  
  /* Only create via the frame itself */
  FromFieldReference(final int sloc, final StackItem obj, final OwnedName f) {
    super(sloc);
    object = obj;
    field = f;
  }
  
  public Type getType() {
    return Type.FIELD_REFERENCE;
  }
  
  public StackItem getObject() {
    return object;
  }
  
  public OwnedName getField() {
    return field;
  }
  
  @Override
  public String toString() {
    return "<" + object + ">." + field.name + "@" + sourceLineOfCode;
  }
}
