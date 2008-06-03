package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class StaticWrapperMethod extends WrapperMethod {
  private static final String WRAPPER_NAME_TEMPLATE = "flashlight${0}${1}${2,choice,0#virtual|1#special|2#static|3#interface}Wrapper";
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "(L{0};{1}Ljava/lang/String;I){2}";
  /** Generated wrapper methods are <code>private static</code> and synthetic. */
  private static final int WRAPPER_METHOD_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

  
  
  public StaticWrapperMethod(final String owner, final String originalName,
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
        WRAPPER_SIGNATURE_TEMPLATE, owner, originalArgs, originalReturn);
  }
  
  @Override
  protected int getAccess() {
    return WRAPPER_METHOD_ACCESS;
  }
  
  @Override
  public void invokeWrapperMethod(
      final MethodVisitor mv, final String classBeingAnalyzed) {
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, classBeingAnalyzed, wrapperName, wrapperSignature);
  }
}
