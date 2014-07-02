package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

final class StaticIndirectAccessMethodInstrumentation extends
    IndirectAccessMethodInstrumentation {
  public StaticIndirectAccessMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final long callSiteId, final MethodInsnNode insn,
      final IndirectAccessMethod am,
      final LocalVariableGenerator vg) {
    super(messenger, classModel, hbt, callSiteId, insn, am, vg);
  }

  @Override
  protected PoppedArguments getPoppedArgs(
      final String owner, final String descriptor,
      final LocalVariableGenerator vg) {
    return PoppedArguments.staticArguments(
        Type.getArgumentTypes(descriptor), vg);
  }

  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    mv.visitInsn(Opcodes.ACONST_NULL);
  }

  @Override
  public void pushArgumentForEvent(final MethodVisitor mv, final int arg) {
    /* arg == 1 is the first argument, so we need to subtract 1 because the
     * receiver is not in the argument list of poppedArgs
     */
    poppedArgs.pushArgument(mv, arg - 1);
  }
}
