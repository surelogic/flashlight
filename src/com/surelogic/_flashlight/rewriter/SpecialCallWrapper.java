package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class SpecialCallWrapper extends MethodCallWrapper {
  private static final String WRAPPER_NAME_TEMPLATE = "flashlight${0}${1}${2,choice,0#virtual|1#special|2#static|3#interface}Wrapper";
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "({0}Ljava/lang/String;I){1}";
  /** Generated wrapper methods are <code>private</code> and synthetic. */
  private static final int WRAPPER_METHOD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
  
  
  public SpecialCallWrapper(final String owner, final String originalName,
      final String originalSignature, final int opcode) {
    super(owner, originalName, originalSignature, opcode);
  }

  
  
  @Override
  protected String createWrapperMethodName(
      final String ownerUnderscored, final String name, final int opcode) {
    return MessageFormat.format(WRAPPER_NAME_TEMPLATE,
        ownerUnderscored, name, (opcode - Opcodes.INVOKEVIRTUAL));
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
  public void invokeWrapperMethod(
      final MethodVisitor mv, final String classBeingAnalyzed) {
    /* Wrapped method is called using invokespecial because it is private */
    mv.visitMethodInsn(
        Opcodes.INVOKESPECIAL, classBeingAnalyzed, wrapperName, wrapperSignature);
  }
}
