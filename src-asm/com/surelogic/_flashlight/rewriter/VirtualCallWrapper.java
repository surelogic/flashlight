package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

final class VirtualCallWrapper extends InterfaceAndVirtualCallWrapper {
  public VirtualCallWrapper(
      final String rcvrTypeInternal,
      final String owner, final String originalName,
      final String originalSignature) {
    super(rcvrTypeInternal, owner, originalName, originalSignature, Opcodes.INVOKEVIRTUAL);
  }
}
