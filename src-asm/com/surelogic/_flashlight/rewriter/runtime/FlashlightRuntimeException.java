package com.surelogic._flashlight.rewriter.runtime;

/**
 * Thrown when something bad happens in the code inserted by the Flashlight
 * translation.
 * 
 * @author aarong
 */
public class FlashlightRuntimeException extends RuntimeException {
  private final Exception cause;
  
  public FlashlightRuntimeException(final Exception c) {
    super("Exception occured in bytecode inserted by Flashlight.");
    cause = c;
  }
  
  /* Don't use getCause() because that is used by JDK 1.4 and up */
  public Exception getOriginalException() {
    return cause;
  }
}
