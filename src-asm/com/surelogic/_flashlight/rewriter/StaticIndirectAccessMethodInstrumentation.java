package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class StaticIndirectAccessMethodInstrumentation extends
    IndirectAccessMethodInstrumentation {
  public StaticIndirectAccessMethodInstrumentation(
      final long callSiteId, final int opcode, final IndirectAccessMethod am,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(callSiteId, opcode, am, owner, name, descriptor, vg);
  }

  @Override 
  protected Type[] getArgumentTypes(final String owner, final String descriptor) {
    return Type.getArgumentTypes(descriptor);
  }

  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    mv.visitInsn(Opcodes.ACONST_NULL);
  }
}
