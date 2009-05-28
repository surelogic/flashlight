package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

final class InstanceIndirectAccessMethodInstrumentation extends
  IndirectAccessMethodInstrumentation {
  public InstanceIndirectAccessMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final long callSiteId, final int opcode, final IndirectAccessMethod am,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(messenger, classModel, callSiteId, opcode, am, owner, name, descriptor, vg);
  }

  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    poppedArgs.pushReceiver(mv);
  }

  @Override
  protected PoppedArguments getPoppedArgs(
      final String owner, final String descriptor,
      final LocalVariableGenerator vg) {
    return PoppedArguments.instanceArguments(
        Type.getObjectType(owner), Type.getArgumentTypes(descriptor), vg);
  }
}
