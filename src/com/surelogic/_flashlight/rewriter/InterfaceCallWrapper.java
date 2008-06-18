package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

final class InterfaceCallWrapper extends InterfaceAndVirtualCallWrapper {
  public InterfaceCallWrapper(final String owner, final String originalName,
      final String originalSignature) {
    super(owner, originalName, originalSignature, Opcodes.INVOKEINTERFACE);
  }
}
