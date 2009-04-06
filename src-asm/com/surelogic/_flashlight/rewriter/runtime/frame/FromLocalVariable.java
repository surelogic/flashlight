package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromLocalVariable extends SourcedStackItem {
  /**
   * The id of the local variable from which the reference was read.  Must
   * not be {@code null}; use {@link LocalVariable#UNKNOWN} if the id is
   * not known.
   */
  private final LocalVariable localVar;

  /**
   * The origin of the object that was read from the local variable.
   */
  private final StackItem objSource;
  
  
  
  /* Only create via the frame itself */
  FromLocalVariable(final int sloc, final LocalVariable lv, final StackItem src) {
    super(sloc);
    localVar = lv;
    objSource = src;
  }

  public Type getType() {
    return Type.LOCAL_VARIABLE;
  }
  
  public LocalVariable getLocalVariable() {
    return localVar;
  }
  
  public StackItem getObjectSource() {
    return objSource;
  }

  @Override
  public String toString() {
    return objSource + " via " + localVar.name + "@" + sourceLineOfCode; 
  }
}
