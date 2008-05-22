package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Type;

/**
 * Class full of utility methods.
 * @author aarong
 *
 */
public final class Utils {
  /** Private method to prevent instantiation. */
  private Utils() {
    // do nothing
  }
  
  
  
  public static String internal2FullyQualified(final String name) {
    return name.replace('/', '.');
  }
  
  public static boolean isCategory2(final String typeDesc) {
    final Type fieldType = Type.getType(typeDesc);
    return fieldType == Type.DOUBLE_TYPE || fieldType == Type.LONG_TYPE;
  }
  
  public static boolean isCategory1(final String typeDesc) {
    return !isCategory2(typeDesc);
  }
}
