package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class StaticCallWrapper extends MethodCallWrapper {
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "({0}Ljava/lang/String;I){1}";
  /** Generated wrapper methods are <code>private static</code> and synthetic. */
  private static final int WRAPPER_METHOD_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
  
  
  
  public StaticCallWrapper(final long callSiteId, final String owner, final String originalName,
      final String originalSignature) {
    super(callSiteId, Opcodes.INVOKESTATIC, owner, originalName, originalSignature, false);
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
  protected int getFirstOriginalArgPosition(int numOriginalArgs) {
    return 0;
  }

  @Override
  protected int getCallingMethodNamePosition(int numOriginalArgs) {
    return numOriginalArgs;
  }

  @Override
  protected int getCallingLineNumberPosition(int numOriginalArgs) {
    return numOriginalArgs + 1;
  }

  @Override
  protected int getWrapperMethodOpcode() {
    return Opcodes.INVOKESTATIC;
  }

  @Override
  public void pushReceiverForEvent(MethodVisitor mv) {
    mv.visitInsn(Opcodes.ACONST_NULL);
  }

  @Override
  protected void pushReceiverForOriginalMethod(MethodVisitor mv) {
    // do nothing
  }

}
