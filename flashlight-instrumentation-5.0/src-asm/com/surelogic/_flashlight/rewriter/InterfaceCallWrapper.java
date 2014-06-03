package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Opcodes;

final class InterfaceCallWrapper extends InterfaceAndVirtualCallWrapper {
  public InterfaceCallWrapper(
      final RewriteMessenger messenger, final ClassAndFieldModel classModel,
      final HappensBeforeTable hbt,
      final String owner, final String originalName,
      final String originalSignature, final boolean itf) {
    super(messenger, classModel, hbt, null, owner, originalName, originalSignature, itf, Opcodes.INVOKEINTERFACE);
  }
}
