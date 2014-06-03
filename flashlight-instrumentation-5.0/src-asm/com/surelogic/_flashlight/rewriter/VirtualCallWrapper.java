package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

final class VirtualCallWrapper extends InterfaceAndVirtualCallWrapper {
  public VirtualCallWrapper(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final String rcvrTypeInternal,
      final String owner, final String originalName,
      final String originalSignature, final boolean itf) {
    super(messenger, classModel, hbt, rcvrTypeInternal, owner, originalName, originalSignature, itf, Opcodes.INVOKEVIRTUAL);
  }
}
