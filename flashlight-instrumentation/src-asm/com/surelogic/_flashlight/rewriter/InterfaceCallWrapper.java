package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.tree.MethodInsnNode;

final class InterfaceCallWrapper extends InterfaceAndVirtualCallWrapper {
  public InterfaceCallWrapper(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final MethodInsnNode insn) {
    super(messenger, classModel, hbt, null, insn);
  }
}
