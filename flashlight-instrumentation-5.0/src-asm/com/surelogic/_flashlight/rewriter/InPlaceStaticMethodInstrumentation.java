package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class InPlaceStaticMethodInstrumentation extends
    InPlaceMethodInstrumentation {

  public InPlaceStaticMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final long callSiteId, final int opcode,
      final String owner, final String name, final String descriptor) {
    super(messenger, classModel, callSiteId, opcode, owner, name, descriptor);
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
  public void pushArgumentForEvent(final MethodVisitor mv, final int arg) {
    // XXX: Problematic if we ever have interesting static methods becase we don't have a record of the arguments
    throw new UnsupportedOperationException("Not implemented for InPlaceStaticMethodInstrumentation");
  }

  @Override
  public void popReceiverAndArguments(final MethodVisitor mv) {
    /* We don't pop anything because we don't need to derive any information
     * from the stack contents.
     */ 
  }
}
