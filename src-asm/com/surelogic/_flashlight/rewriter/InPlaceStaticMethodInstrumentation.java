package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class InPlaceStaticMethodInstrumentation extends
    InPlaceMethodInstrumentation {

  public InPlaceStaticMethodInstrumentation(final int opcode,
      final String owner, final String name, final String descriptor,
      final String callingName, final int lineNumber) {
    super(opcode, owner, name, descriptor, callingName, lineNumber);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void pushReceiverAndArguments(final MethodVisitor mv) {
    /* Don't need to do anything because we never popped the original 
     * arguments from the stack.
     */ 
  }

  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    mv.visitInsn(Opcodes.ACONST_NULL);
  }

  @Override
  public void popReceiverAndArguments(final MethodVisitor mv) {
    /* We don't pop anything because we don't need to derive any information
     * from the stack contents.
     */ 
  }
}
