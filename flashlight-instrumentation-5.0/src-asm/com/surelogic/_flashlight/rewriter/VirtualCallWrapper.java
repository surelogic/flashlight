package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

final class VirtualCallWrapper extends InterfaceAndVirtualCallWrapper {
  public VirtualCallWrapper(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final String rcvrTypeInternal, 
      final String owner, final String originalName,
      final String originalSignature, final boolean itf, final AbstractInsnNode insn) {
    super(messenger, classModel, hbt, rcvrTypeInternal, owner, originalName, originalSignature, itf, Opcodes.INVOKEVIRTUAL, insn);
  }
}
