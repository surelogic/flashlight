package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromArrayReference extends SourcedStackItem {
  private final StackItem object;
  private final int index;
  
  /* Only create via the frame itself */
  FromArrayReference(final int sloc, final StackItem obj, final int idx) {
    super(sloc);
    object = obj;
    index = idx;
  }
  
  public Type getType() {
    return Type.ARRAY_REFERENCE;
  }
  
  public StackItem getObject() {
    return object;
  }
  
  public int getIndex() {
    return index;
  }
  
  @Override
  public String toString() {
    return "<" + object + ">[" + index + "]@" + sourceLineOfCode;
  }
}
