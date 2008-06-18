package com.surelogic._flashlight.rewriter;

/**
 * Thrown when something bad happens during rewriting.
 */
public class FlashlightRewriteException extends RuntimeException {
  public FlashlightRewriteException(final String msg) {
    super(msg);
  }
}
