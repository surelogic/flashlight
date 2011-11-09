package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Type;

interface LocalVariableGenerator {
  /**
   * Return a fresh local variable index for a local variable capable of 
   * holding a value of the given type.
   */
  public int newLocal(Type type);
}
