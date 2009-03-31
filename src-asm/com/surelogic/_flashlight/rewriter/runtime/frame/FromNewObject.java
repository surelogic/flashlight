package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromNewObject extends SourcedStackItem {
  /**
   * The type of the new object
   */
  private final String description;
  
  /* Only create via the frame itself */
  FromNewObject(final int sloc, final String desc) {
    super(sloc);
    description = desc;
  }
  
  public Type getType() {
    return Type.NEW_OBJECT;
  }
  
  public String getDescripton() {
    return description;
  }
  
  @Override
  public String toString() {
    return "new(" + description + ")@" + sourceLineOfCode;
  }
}
