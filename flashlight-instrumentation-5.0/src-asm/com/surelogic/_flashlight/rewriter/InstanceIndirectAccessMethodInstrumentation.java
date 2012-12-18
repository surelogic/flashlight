package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

final class InstanceIndirectAccessMethodInstrumentation extends
  IndirectAccessMethodInstrumentation {
  public InstanceIndirectAccessMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final long callSiteId, final int opcode, final IndirectAccessMethod am,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(messenger, classModel, hbt, callSiteId, opcode, am, owner, name, descriptor, vg);
  }

  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    poppedArgs.pushReceiver(mv);
  }

  @Override
  public void pushArgumentForEvent(final MethodVisitor mv, final int arg) {
    /* arg == 1 is the first argument, so we do not have to correct because
     * we have the receiver in the argument list of poppedArgs. 
     */
    poppedArgs.pushArgument(mv, arg);
  }
  
  @Override
  protected PoppedArguments getPoppedArgs(
      final String owner, final String descriptor,
      final LocalVariableGenerator vg) {
    return PoppedArguments.instanceArguments(
        Type.getObjectType(owner), Type.getArgumentTypes(descriptor), vg);
  }
}
