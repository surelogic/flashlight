package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromException extends SourcedStackItem {
  private final String type;
  
  /* Only create via the frame itself */
  FromException(final int sloc, final String t) {
    super(sloc);
    type = t;
  }
  
  public Type getType() {
    return Type.EXCEPTION;
  }
  
  public String getException() {
    return type;
  }
  
  @Override
  public String toString() {
    return "Caught " + type + "@" + sourceLineOfCode;
  }
}
