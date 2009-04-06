package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromReceiver implements StackItem {
  public static final FromReceiver PROTOTYPE = new FromReceiver();

  /** Use the prototype */
  private FromReceiver() {
    // do nothing
  }
  
  public Type getType() {
    return Type.RECEIVER;
  }

  @Override
  public String toString() {
    return "this";
  }      
}
