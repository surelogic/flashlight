package com.surelogic._flashlight.rewriter.runtime.frame;

public class FromStaticMethodCall extends SourcedStackItem {
  private final OwnedName method;
  
  /* Only create via the frame itself */
  FromStaticMethodCall(final int sloc, final OwnedName m) {
    super(sloc);
    method = m;
  }
  
  public Type getType() {
    return Type.METHOD_CALL;
  }
  
  public OwnedName getMethod() {
    return method;
  }
  
  @Override
  public String toString() {
    return method.owner + "." + method.name + "()@" + sourceLineOfCode;
  }
}
