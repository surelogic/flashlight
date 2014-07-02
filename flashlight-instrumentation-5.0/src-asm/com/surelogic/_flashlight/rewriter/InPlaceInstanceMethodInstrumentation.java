package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

class InPlaceInstanceMethodInstrumentation extends
    InPlaceMethodInstrumentation {
  private final PoppedArguments poppedArgs;
  
  public InPlaceInstanceMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final long callSiteId, final AbstractInsnNode insn, final int opcode,
      final String owner, final String name, final String descriptor, final boolean itf,
      final LocalVariableGenerator vg) {
    super(messenger, classModel, hbt,callSiteId, insn, opcode, owner, name, descriptor, itf);
    poppedArgs = PoppedArguments.instanceArguments(
        Type.getObjectType(owner), Type.getArgumentTypes(descriptor), vg);
  }

  @Override
  public void popReceiverAndArguments(final MethodVisitor mv) {
    poppedArgs.popReceiverAndArguments(mv);
  }

  @Override
  public void pushReceiverAndArguments(final MethodVisitor mv) {
    poppedArgs.pushReceiverAndArguments(mv);
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
}
