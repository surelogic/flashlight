package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

public final class FlashlightAttribute extends Attribute {
  private static final String NAME = "com.surelogic.flashlight.Instrumented";
  
  protected FlashlightAttribute() {
    super(NAME);
  }

  @Override
  public boolean isUnknown() {
    return false;
  } 
  
  @Override
  protected FlashlightAttribute read(
        final ClassReader cr, final int off, final int len,
        final char[] buf, final int codeOff, final Label[] labels) {
    return new FlashlightAttribute();
  }
  
  @Override
  protected ByteVector write(
      final ClassWriter cw, final byte[] code,  final int len,
      final int maxStack, final int maxLocals) {
    return new ByteVector(0);
  }
}
