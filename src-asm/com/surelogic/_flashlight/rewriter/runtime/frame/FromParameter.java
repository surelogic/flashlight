package com.surelogic._flashlight.rewriter.runtime.frame;

/**
 * The value in the a parameter at the start of method execution, representing
 * the value was originally passed to the method.  If the parameter is assigned
 * to during method execution this value is lost.
 */
public final class FromParameter implements StackItem {
  private final int argIdx;
  
  /* Only create via the Frame */
  FromParameter(final int arg) {
    argIdx = arg;
  }
  
  public Type getType() {
    return Type.PARAMETER;
  }
  
  public int getIndex() {
    return argIdx;
  }

  @Override
  public String toString() {
    return "Arg " + argIdx;
  }      
}
