package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class InPlaceInstanceMethodInstrumentation extends
    InPlaceMethodInstrumentation {
  private final PoppedArguments poppedArgs;
  
  public InPlaceInstanceMethodInstrumentation(final long callSiteId, final int opcode,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(callSiteId, opcode, owner, name, descriptor);
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
}
