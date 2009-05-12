package com.surelogic._flashlight.rewriter;

public final class ClassNameUtil {
  private ClassNameUtil() {
    // Do nothing
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
   * Convert a fully qualified class name to an internal class name.
   * 
   * @param name
   *          An fully qualified class name
   * @return The internal class name that corresponds to the given name.
   */
  public static String fullyQualified2Internal(final String name) {
    return name.replace('.', '/');
  }
}
