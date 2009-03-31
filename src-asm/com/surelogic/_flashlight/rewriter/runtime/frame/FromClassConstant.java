package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromClassConstant extends SourcedStackItem {
  private final String clazz;
  
  /* Only create via the frame itself */
  FromClassConstant(final int sloc, final String c) {
    super(sloc);
    clazz = c;
  }
  
  public Type getType() {
    return Type.STRING_CONSTANT;
  }
  
  public String getClazz() {
    return clazz;
  }
  
  @Override
  public String toString() {
    return clazz + ".this@" + sourceLineOfCode;
  }
}
