package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;

import com.surelogic._flashlight.rewriter.config.Configuration;

abstract class IndirectAccessMethodInstrumentation extends
    InPlaceMethodInstrumentation {
  final IndirectAccessMethod indirectAccess;
  final PoppedArguments poppedArgs;
  
  public IndirectAccessMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final long callSiteId, final int opcode, final IndirectAccessMethod am,
      final String owner, final String name, final String descriptor, final boolean itf,
      final LocalVariableGenerator vg) {
    super(messenger, classModel, hbt, callSiteId, opcode, owner, name, descriptor, itf);
    indirectAccess = am;
    poppedArgs = getPoppedArgs(owner, descriptor, vg);
  }

  protected abstract PoppedArguments getPoppedArgs(
      String owner, String descriptor, LocalVariableGenerator vg);
  
  @Override
  public final void popReceiverAndArguments(final MethodVisitor mv) {
    poppedArgs.popReceiverAndArguments(mv);
  }

  @Override
  public final void pushReceiverAndArguments(final MethodVisitor mv) {
    poppedArgs.pushReceiverAndArguments(mv);
  }

  
  
  public final void recordIndirectAccesses(
      final MethodVisitor mv, final Configuration config) {
    indirectAccess.callStore(mv, config, callSiteId, poppedArgs.getLocals());
  }
}
