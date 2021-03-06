package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

abstract class InterfaceAndVirtualCallWrapper extends MethodCallWrapper {
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "({0}{1}J){2}";
  /** Generated wrapper methods are <code>private static</code> and synthetic. */
  private static final int WRAPPER_METHOD_ACCESS =
    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

  
  
  public InterfaceAndVirtualCallWrapper(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final String rcvrTypeInternal,
      final MethodInsnNode insn) {
    super(messenger, classModel, hbt, insn, rcvrTypeInternal, false);
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
  protected final int getSiteIdArgPosition(final int numOriginalArgs) {
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
