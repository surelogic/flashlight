package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class InPlaceInstanceMethodInstrumentation extends
    InPlaceMethodInstrumentation {
  private final Type rcvrType;
  private int rcvrLocal;
  final Type[] argTypes;
  final int[] argLocals;
  final LocalVariableGenerator varGenerator;
  
  public InPlaceInstanceMethodInstrumentation(final long callSiteId, final int opcode,
      final String owner, final String name, final String descriptor,
      final LocalVariableGenerator vg) {
    super(callSiteId, opcode, owner, name, descriptor);
    this.varGenerator = vg;
    this.rcvrType = Type.getObjectType(owner);
    this.rcvrLocal = -1;
    this.argTypes = Type.getArgumentTypes(descriptor);
    this.argLocals = new int[argTypes.length];
    for (int i = 0; i < argTypes.length; i++) argLocals[i] = -1;
  }

  @Override
  public void popReceiverAndArguments(final MethodVisitor mv) {
    /* First allocate the local variables we need */
    rcvrLocal = varGenerator.newLocal(rcvrType);
    for (int i = 0; i < argTypes.length; i++) {
      argLocals[i] = varGenerator.newLocal(argTypes[i]);
    }

    // ..., rcvr, [args]

    /* Pop the arguments: the last argument is the first on the stack */
    for (int i = argTypes.length-1; i >= 0; i--) {
      mv.visitVarInsn(argTypes[i].getOpcode(Opcodes.ISTORE), argLocals[i]);
    }
    // ..., rcvr
    
    /* Pop the receiver */
    mv.visitVarInsn(rcvrType.getOpcode(Opcodes.ISTORE), rcvrLocal);
    // ...
  }

  @Override
  public void pushReceiverAndArguments(final MethodVisitor mv) {
    // ...
    
    /* Push the receiver */
    mv.visitVarInsn(rcvrType.getOpcode(Opcodes.ILOAD), rcvrLocal);
    // ..., rcvr
    
    /* Push the arguments */
    for (int i = 0; i < argTypes.length; i++) {
      mv.visitVarInsn(argTypes[i].getOpcode(Opcodes.ILOAD), argLocals[i]);
    }
    // ..., rcvr, [args]
  }

  @Override
  public void pushReceiverForEvent(final MethodVisitor mv) {
    mv.visitVarInsn(rcvrType.getOpcode(Opcodes.ILOAD), rcvrLocal);
  }

}
