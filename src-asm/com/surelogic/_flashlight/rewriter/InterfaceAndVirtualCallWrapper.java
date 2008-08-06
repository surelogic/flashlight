package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

abstract class InterfaceAndVirtualCallWrapper extends MethodCallWrapper {
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "({0}{1}Ljava/lang/String;I){2}";
  /** Generated wrapper methods are <code>private static</code> and synthetic. */
  private static final int WRAPPER_METHOD_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

  
  
  public InterfaceAndVirtualCallWrapper(final String owner, final String originalName,
      final String originalSignature, final int opcode) {
    super(opcode, owner, originalName, originalSignature, false);
  }

  
  
  @Override
  protected String createWrapperMethodSignature(
      final String owner, final String originalArgs, final String originalReturn) {
    return MessageFormat.format(
        WRAPPER_SIGNATURE_TEMPLATE, owner, originalArgs, originalReturn);
  }
  
  @Override
  protected int getAccess() {
    return WRAPPER_METHOD_ACCESS;
  }
  
  @Override
  protected int getWrapperMethodOpcode() {
    return Opcodes.INVOKESTATIC;
  }


  
  @Override
  protected final int getFirstOriginalArgPosition(final int numOriginalArgs) {
    return 1;
  }

  @Override
  protected final int getCallingMethodNamePosition(final int numOriginalArgs) {
    return numOriginalArgs + 1;
  }
  
  @Override
  protected final int getCallingLineNumberPosition(final int numOriginalArgs) {
    return numOriginalArgs + 2;
  }

  @Override
  public final void pushReceiverForEvent(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, 0);
  }

  @Override
  protected final void pushReceiverForOriginalMethod(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, 0);
  }
}
