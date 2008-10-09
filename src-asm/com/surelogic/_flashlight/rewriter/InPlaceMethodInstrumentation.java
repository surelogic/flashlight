package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;

/**
 * Abstract representation of a method call that will instrumented in place,
 * and not by calling an newly generate wrapper method.  We need to do this for
 * method calls that are found inside of the static initializer of interfaces.
 * We could do it for all cases, but it causes code bloat.
 */
abstract class InPlaceMethodInstrumentation extends MethodCall {
  /**
   * 
   * @param opcode The opcode used to invoke the method.
   * @param owner The owner of the method.
   * @param name The name of the method.
   * @param descriptor The descriptor of the method.
   * @param callingName The name of the calling method.
   * @param lineNumber The source line of the method call.
   */
  public InPlaceMethodInstrumentation(final long callSiteId, final int opcode,
      final String owner, final String name, final String descriptor) {
    super(callSiteId, opcode, owner, name, descriptor);
  }
  
  public abstract void popReceiverAndArguments(final MethodVisitor mv);
}
