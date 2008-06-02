package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class ByteCodeUtils {
  private ByteCodeUtils() {
    // do nothing
  }
  
  
  
  /**
   * Generate code to push an integer constant.  Optimizes for whether the
   * integer fits in 8, 16, or 32 bits.
   * @param v The integer to push onto the stack.
   */
  public static void pushIntegerConstant(final MethodVisitor mv, final int v) {
    if (v >= -1 && v <= 5) {
      mv.visitInsn(Opcodes.ICONST_0 + v);
    } else if (v >= -128 && v <= 127) {
      mv.visitIntInsn(Opcodes.BIPUSH, v);
    } else if (v >= -32768 && v <= 32767) {
      mv.visitIntInsn(Opcodes.SIPUSH, v);
    } else {
      mv.visitLdcInsn(Integer.valueOf(v));
    }
  }
  
  /**
   * Generate code to push a Boolean constant.
   * @param b The Boolean value to push onto the stack.
   */
  public static void pushBooleanConstant(final MethodVisitor mv, final boolean b) {
    if (b) {
      mv.visitInsn(Opcodes.ICONST_1);
    } else {
      mv.visitInsn(Opcodes.ICONST_0);
    }
  }

  /**
   * Generate code to push the Class object of the class that this method
   * is a part of.
   */
  public static void pushInClass(final MethodVisitor mv, final String internalClassName) {
    mv.visitFieldInsn(Opcodes.GETSTATIC, internalClassName,
        FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
  }
}
