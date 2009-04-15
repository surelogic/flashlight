package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class InstanceIndirectAccessMethodInstrumentation extends
  IndirectAccessMethodInstrumentation {
  public InstanceIndirectAccessMethodInstrumentation(
      final long callSiteId, final int opcode, final IndirectAccessMethod am,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(callSiteId, opcode, am, owner, name, descriptor, vg);
  }

  @Override
  protected Type[] getArgumentTypes(final String owner, final String descriptor) {
    final Type[] argTypes = Type.getArgumentTypes(descriptor);
    final Type[] allArgTypes = new Type[1 + argTypes.length];
    allArgTypes[0] = Type.getObjectType(owner);
    System.arraycopy(argTypes, 0, allArgTypes, 1, argTypes.length);
    return allArgTypes;
  }
  
  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, argLocals[0]);
  }
}
