package com.surelogic._flashlight.rewriter;

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
}
