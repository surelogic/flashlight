package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Abstract representation of a method call that will instrumented in place,
 * and not by calling an newly generate wrapper method.  We need to do this for
 * method calls that are found inside of the static initializer of interfaces.
 * We could do it for all cases, but it causes code bloat.
 */
abstract class InPlaceMethodInstrumentation extends MethodCall {
  final static class PoppedArguments {
    private final LocalVariableGenerator varGenerator;
    private final Type[] argTypes;
    private final int[] argLocals;

    
    
    private PoppedArguments(final Type[] types, final LocalVariableGenerator vg) {
      argTypes = types;
      argLocals = new int[types.length];
      varGenerator = vg;
    }
    
    public static PoppedArguments staticArguments(
        final Type[] types, final LocalVariableGenerator vg) {
      return new PoppedArguments(types, vg);
    }
    
    public static PoppedArguments instanceArguments(
        final Type rtype, final Type[] types, final LocalVariableGenerator vg) {
      final Type[] allTypes = new Type[types.length+1];
      allTypes[0] = rtype;
      System.arraycopy(types, 0, allTypes, 1, types.length);
      return new PoppedArguments(allTypes, vg);
    }

    
    
    public void popReceiverAndArguments(final MethodVisitor mv) {
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

    public void pushReceiverAndArguments(final MethodVisitor mv) {
      // ...
      for (int i = 0; i < argLocals.length; i++) {
        mv.visitVarInsn(argTypes[i].getOpcode(Opcodes.ILOAD), argLocals[i]);
      }
      // ..., [args]
    }
    
    public void pushReceiver(final MethodVisitor mv) {
      mv.visitVarInsn(Opcodes.ALOAD, argLocals[0]);
    }
    
    public void pushArgument(final MethodVisitor mv, final int offset) {
      // offset is already corrected for the presense of the receiver or not
      mv.visitVarInsn(argTypes[offset].getOpcode(Opcodes.ILOAD), argLocals[offset]);
    }
    
    public int[] getLocals() {
      return argLocals;
    }
  }
  
  
  
  final long callSiteId;

  /**
   * 
   * @param opcode The opcode used to invoke the method.
   * @param owner The owner of the method.
   * @param name The name of the method.
   * @param descriptor The descriptor of the method.
   * @param callingName The name of the calling method.
   * @param lineNumber The source line of the method call.
   */
  public InPlaceMethodInstrumentation(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final long callSiteId, final MethodInsnNode insn) {
    super(messenger, classModel, hbt, insn);
    this.callSiteId = callSiteId;
  }
  
  public abstract void popReceiverAndArguments(final MethodVisitor mv);
  
  @Override
  public final void pushSiteId(final MethodVisitor mv) {
    ByteCodeUtils.pushLongConstant(mv, callSiteId);
  }
}
