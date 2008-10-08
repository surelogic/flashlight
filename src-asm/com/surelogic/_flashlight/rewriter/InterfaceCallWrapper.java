package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

final class InterfaceCallWrapper extends InterfaceAndVirtualCallWrapper {
  public InterfaceCallWrapper(final long callSiteId, final String owner, final String originalName,
      final String originalSignature) {
    super(callSiteId, owner, originalName, originalSignature, Opcodes.INVOKEINTERFACE);
  }
}
