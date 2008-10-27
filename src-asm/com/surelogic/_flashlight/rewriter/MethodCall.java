package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.MethodVisitor;

/**
 * Abstract representation of a method call that needs to be instrumented.
 * Generic enough to accommodate both method calls that are instrumented by being
 * placed in wrapper methods and to and method calls that are instrumented 
 * in place (in the case of calls from interface initializers).
 */
abstract class MethodCall {
  protected final int opcode;
  protected final String owner;
  protected final String name;
  protected final String descriptor;
  
  
  
  /**
   * @param opcode The opcode used to invoke the method.
   * @param owner The owner of the method.
   * @param originalName The name of the method.
   * @param originalDesc The descriptor of the method.
   */
  public MethodCall(final int opcode, final String owner,
      final String originalName, final String originalDesc) {
    this.opcode = opcode;
    this.owner = owner;
    this.name = originalName;
    this.descriptor = originalDesc;
  }

  
  
  public final boolean testCalledMethodName(
      final String testOwner, final String testName) {
    return owner.equals(testOwner) && name.equals(testName);
  }

  /**
   * Push the call site identifier.
   */
  public abstract void pushSiteId(MethodVisitor mv);
  
  /**
   * Pushes the receiver object on to the stack for use by an instrumentation
   * event method.  Must push <code>null</code> if the method is static.
   * @param mv
   */
  public abstract void pushReceiverForEvent(MethodVisitor mv);
  
  /**
   * Push the original receiver and original arguments onto the stack so that
   * method can be invoked.  This is separate from invoking the method via
   * {@link #invokeMethod(MethodVisitor)} so that the labels for the try-finally
   * block can be inserted around the method invocation.  This keeps the block
   * from being larger than necessary.
   */
  public abstract void pushReceiverAndArguments(MethodVisitor mv);
  
  public final void invokeMethod(final MethodVisitor mv) {
    mv.visitMethodInsn(opcode, owner, name, descriptor);
  }
}
