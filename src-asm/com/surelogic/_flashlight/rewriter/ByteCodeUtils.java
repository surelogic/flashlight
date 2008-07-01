package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Home to static utility functions for generation of bytecode operations.
 */
final class ByteCodeUtils {
  private ByteCodeUtils() {
    // do nothing
  }
  
  
  
  /**
   * Convert an internal class name to a fully qualified class name.
   * 
   * @param name
   *          An internal class name
   * @return The fully qualified class name that corresponds to the given name.
   */
  public static String internal2FullyQualified(final String name) {
    return name.replace('/', '.');
  }
  
  /**
   * Test if the given type description is for a category 2 type.
   * 
   * @param typeDesc
   *          The type description to test.
   * @return <code>true</code> if the described type is category 2;
   *         <code>false</code> otherwise, i.e., the described type is
   *         category 1.
   */
  public static boolean isCategory2(final String typeDesc) {
    final Type fieldType = Type.getType(typeDesc);
    return fieldType == Type.DOUBLE_TYPE || fieldType == Type.LONG_TYPE;
  }
    
  /**
   * Generate code to push an integer constant. Optimizes for whether the
   * integer fits in 8, 16, or 32 bits.
   * 
   * @param v
   *          The integer to push onto the stack.
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
   * 
   * @param b
   *          The Boolean value to push onto the stack.
   */
  public static void pushBooleanConstant(final MethodVisitor mv, final boolean b) {
    if (b) {
      mv.visitInsn(Opcodes.ICONST_1);
    } else {
      mv.visitInsn(Opcodes.ICONST_0);
    }
  }

  /**
   * Generate code to push the Class object the named class.
   */
  public static void pushInClass(
      final MethodVisitor mv, final boolean atLeastJava5, 
      final String internalClassName) {
    if (!atLeastJava5) {
      mv.visitFieldInsn(Opcodes.GETSTATIC, internalClassName,
          FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    } else {
      mv.visitLdcInsn(Type.getType("L"+internalClassName+";"));
    }
  }

  /**
   * Get the name of the flashlight store class.
   */
  public static final String getFlashlightStore(final Configuration config) {
    if (config.useDebugStore) {
      return FlashlightNames.FLASHLIGHT_DEBUG_STORE;
    } else {
      return FlashlightNames.FLASHLIGHT_STORE;
    }
  }}
