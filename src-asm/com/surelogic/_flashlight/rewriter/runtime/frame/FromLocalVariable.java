package com.surelogic._flashlight.rewriter.runtime.frame;

public final class FromLocalVariable extends SourcedStackItem {
//  /**
//   * The id of the local variable from which the reference was read.  Must
//   * not be {@code null}; use {@link LocalVariable#UNKNOWN} if the id is
//   * not known.
//   */
//  private final LocalVariable localVar;

  private final int index;
  
//  /* Only create via the frame itself */
//  FromLocalVariable(final int sloc, final LocalVariable lv) {
//    super(sloc);
//    localVar = lv;
//  }

  FromLocalVariable(final int sloc, final int idx) {
    super(sloc);
    index = idx;
  }

  public Type getType() {
    return Type.LOCAL_VARIABLE;
  }
  
//  public LocalVariable getLocalVariable() {
//    return localVar;
//  }

  public int getIndex() {
    return index;
  }
  
  @Override
  public String toString() {
    return "lv" + index + "@" + sourceLineOfCode;
  }
}
