package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromStringConstant extends SourcedStackItem {
  private final String value;
  
  /* Only create via the frame itself */
  FromStringConstant(final int sloc, final String v) {
    super(sloc);
    value = v;
  }
  
  public Type getType() {
    return Type.STRING_CONSTANT;
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return "\"" + value + "\"@" + sourceLineOfCode;
  }
}
