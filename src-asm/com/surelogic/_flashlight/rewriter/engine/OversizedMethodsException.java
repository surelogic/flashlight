package com.surelogic._flashlight.rewriter.engine;

import java.util.Set;

import com.surelogic._flashlight.rewriter.MethodIdentifier;

public final class OversizedMethodsException extends Exception {
  private Set<MethodIdentifier> oversizedMethods;
  
  public OversizedMethodsException(
      final String message, final Set<MethodIdentifier> methods) {
    super(message);
    oversizedMethods = methods;
  }
  
  public Set<MethodIdentifier> getOversizedMethods() {
    return oversizedMethods;
  }
}
