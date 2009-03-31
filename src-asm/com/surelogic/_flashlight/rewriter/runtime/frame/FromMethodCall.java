package com.surelogic._flashlight.rewriter.runtime.frame;

public class FromMethodCall extends SourcedStackItem {
  private final StackItem object;
  /** Opcode from {@link org.objectweb.asm.Opcodes}. */
  private final int opcode;
  private final OwnedName method;
  
  /* Only create via the frame itself */
  FromMethodCall(final int sloc,
      final int op, final StackItem obj, final OwnedName m) {
    super(sloc);
    object = obj;
    opcode = op;
    method = m;
  }
  
  public Type getType() {
    return Type.METHOD_CALL;
  }

  public StackItem getObject() {
    return object;
  }
  
  public int getOpcode() {
    return opcode;
  }
  
  public OwnedName getMethod() {
    return method;
  }
  
  @Override
  public String toString() {
    return "<" + object + ">." + method.name + "()@" + sourceLineOfCode;
  }
}
