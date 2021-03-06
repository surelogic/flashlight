package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

final class InPlaceStaticMethodInstrumentation extends
    InPlaceMethodInstrumentation {

  public InPlaceStaticMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final long callSiteId, final MethodInsnNode insn) {
    super(messenger, classModel, hbt, callSiteId, insn);
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
