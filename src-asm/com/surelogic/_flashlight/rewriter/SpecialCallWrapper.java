package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class SpecialCallWrapper extends MethodCallWrapper {
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "({0}Ljava/lang/String;I){1}";
  /** Generated wrapper methods are <code>private</code> and synthetic. */
  private static final int WRAPPER_METHOD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
  
  
  
  public SpecialCallWrapper(
      final String owner, final String originalName, final String originalSignature) {
    super(Opcodes.INVOKESPECIAL, owner, originalName, originalSignature, true);
  }

  
  
  @Override
  protected String createWrapperMethodSignature(
      final String owner, final String originalArgs, final String originalReturn) {
    return MessageFormat.format(
        WRAPPER_SIGNATURE_TEMPLATE, originalArgs, originalReturn);
  }
  
  @Override
  protected int getAccess() {
    return WRAPPER_METHOD_ACCESS;
  }
  
  @Override
  protected int getWrapperMethodOpcode() {
    return Opcodes.INVOKESPECIAL;
  }

  
  
  @Override
  protected final int getFirstOriginalArgPosition(final int numOriginalArgs) {
    return 0;
  }

  @Override
  protected final int getCallingMethodNamePosition(final int numOriginalArgs) {
    return numOriginalArgs;
  }
  
  @Override
  protected final int getCallingLineNumberPosition(final int numOriginalArgs) {
    return numOriginalArgs + 1;
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
