package com.surelogic._flashlight.rewriter.runtime.frame;

abstract class SourcedStackItem implements StackItem {
  protected final int sourceLineOfCode;

  public SourcedStackItem(final int sloc) {
    sourceLineOfCode = sloc;
  }
  
  /* Force subclasses to reimplement toString() */
  @Override
  public abstract String toString();
}
