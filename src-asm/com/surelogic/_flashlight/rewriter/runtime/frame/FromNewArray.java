package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromNewArray extends SourcedStackItem {
  /**
   * The type of the array elements
   */
  private final String description;
  
  /* Only create via the frame itself */
  FromNewArray(final int sloc, final String desc) {
    super(sloc);
    description = desc;
  }
  
  public Type getType() {
    return Type.NEW_ARRAY;
  }
  
  public String getDescripton() {
    return description;
  }
  
  @Override
  public String toString() {
    return "NEW_ARRAY@" + sourceLineOfCode;
  }
}
