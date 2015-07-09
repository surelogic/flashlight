package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.tree.MethodInsnNode;

final class VirtualCallWrapper extends InterfaceAndVirtualCallWrapper {
  public VirtualCallWrapper(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final String rcvrTypeInternal, 
      final MethodInsnNode insn) {
    super(messenger, classModel, hbt, rcvrTypeInternal, insn);
  }
}
