package com.surelogic._flashlight.rewriter.runtime.frame;

public final class IsPrimitive implements StackItem {
  public static final IsPrimitive PROTOTYPE = new IsPrimitive();
  
  /* Use the prototype */
  private IsPrimitive() {
    super();
  }
  
  public Type getType() {
    return Type.PRIMITIVE;
  }
  
  @Override
  public String toString() {
    return "primitive";
  }
}
