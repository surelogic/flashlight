package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

abstract class IndirectAccessMethodInstrumentation extends
    InPlaceMethodInstrumentation {
  private final IndirectAccessMethod indirectAccess;
  final Type[] argTypes;
  final int[] argLocals;
  final LocalVariableGenerator varGenerator;
  
  public IndirectAccessMethodInstrumentation(
      final long callSiteId, final int opcode, final IndirectAccessMethod am,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(callSiteId, opcode, owner, name, descriptor);
    indirectAccess = am;
    this.varGenerator = vg;
    this.argTypes = getArgumentTypes(owner, descriptor);
    this.argLocals = new int[argTypes.length];
  }

  protected abstract Type[] getArgumentTypes(String owner, String descriptor);
  
  @Override
  public final void popReceiverAndArguments(final MethodVisitor mv) {
    /* First allocate the local variables we need */
    for (int i = 0; i < argLocals.length; i++) {
      argLocals[i] = varGenerator.newLocal(argTypes[i]);
    }

    // ..., [args]

    /* Pop the arguments: the last argument is the first on the stack */
    for (int i = argLocals.length-1; i >= 0; i--) {
      mv.visitVarInsn(argTypes[i].getOpcode(Opcodes.ISTORE), argLocals[i]);
    }
    // ...
  }

  @Override
  public final void pushReceiverAndArguments(final MethodVisitor mv) {
    // ...
    for (int i = 0; i < argLocals.length; i++) {
      mv.visitVarInsn(argTypes[i].getOpcode(Opcodes.ILOAD), argLocals[i]);
    }
    // ..., [args]
  }

  
  
  public final void recordIndirectAccesses(
      final MethodVisitor mv, final Configuration config) {
    indirectAccess.callStore(mv, config, callSiteId, argLocals);
  }
}
