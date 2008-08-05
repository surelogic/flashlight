package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class InPlaceInstanceMethodInstrumentation extends
    InPlaceMethodInstrumentation {
  private final Type rcvrType;
  private int rcvrLocal;
  final Type[] argTypes;
  final int[] argLocals;
  final LocalVariablesSorter varSorter;
  
  public InPlaceInstanceMethodInstrumentation(final int opcode,
      final String owner, final String name, final String descriptor,
      final String callingName, final int lineNumber,
      final LocalVariablesSorter lvs) {
    super(opcode, owner, name, descriptor, callingName, lineNumber);
    this.varSorter = lvs;
    this.rcvrType = Type.getObjectType(owner);
    this.rcvrLocal = -1;
    this.argTypes = Type.getArgumentTypes(descriptor);
    this.argLocals = new int[argTypes.length];
    for (int i = 0; i < argTypes.length; i++) argLocals[i] = -1;
  }

  @Override
  public void popReceiverAndArguments(final MethodVisitor mv) {
    /* First allocate the local variables we need */
    rcvrLocal = varSorter.newLocal(rcvrType);
    for (int i = 0; i < argTypes.length; i++) {
      argLocals[i] = varSorter.newLocal(argTypes[i]);
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
