package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

final class VirtualCallWrapper extends InterfaceAndVirtualCallWrapper {
  public VirtualCallWrapper(final String owner, final String originalName,
      final String originalSignature) {
    super(owner, originalName, originalSignature, Opcodes.INVOKEVIRTUAL);
  }
}
