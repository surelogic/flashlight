package com.surelogic._flashlight.rewriter.runtime.frame;

public final class BogusFrame {
  public BogusFrame(final int numLocals, final int stackSize) {
    // do nothing
  }
  
  
  
  /**
   * Forget the identity of a local variable.
   */
  public void clearLocalVariable(final int index) {
    // do nothing
  }
  
  /**
   * Set (or reset) the identity of a local variable.
   */
  public void setLocalVariable(
      final int index, final String name, final String description) {
    // do nothing
  }
  
  /**
   * Initialize the receiver. 
   */
  public void initReceiver() {
    // do nothing
  }
  
  
  /**
   * Initialize a local variable holding a parameter value.
   */
  public void initParameter(final int localIdx, final int argIdx) {
    // do nothing
  }
  
  
  
  /**
   * Set the current line number.
   */
  public void setCurrentSourceLine(final int line) {
    // do nothing
  }
  
  
  
  // ======================================================================
  // == Handle specific opcodes
  // ======================================================================
  
  public void aaload() {
    // do nothing
  }
  
  public void aload(final int localIdx) {
    // do nothing
  }
  
  public void arraylength() {
    // do nothing
  }

  public void astore(final int localIdx) {
    // do nothing
  }
  
  public void athrow() {
    // do nothing
  }
  
  public void dup() {
    // do nothing
  }
  
  public void dup_x1() {
    // do nothing
  }

  public void dup_x2() {
    // do nothing
  }

  public void dup2() {
    // do nothing
  }
  
  public void dup2_x1() {
    // do nothing
  }

  public void dup2_x2() {
    // do nothing
  }

  public void getfieldObject(
      final String owner, final String name, final String description) {
    // do nothing
  }

  public void getfieldPrimitive() {
    // do nothing
  }

  public void getfieldPrimitive2() {
    // do nothing
  }

  public void getstaticObject(
      final String owner, final String name, final String description) {
    // do nothing
  }
  
  public void getstaticPrimitive() {
    // do nothing
  }
  
  public void getstaticPrimitive2() {
    // do nothing
  }
  
  public void instanceOf() {
    // do nothing
  }
  
  public void ldcString(final String value) {
    // do nothing
  }
  
  public void ldcClass(final String classname) {
    // do nothing
  }
  
  /**
   * @param elementType The type of the array elements.  Must be derived from
   *  the type in the raw instruction.
   * @param dims The number of dimensions on the stack
   */
  public void multianewarray(final String elementType, final int dims) {
    // do nothing
  }
  
  public void newObject(final String description) {
    // do nothing
  }
  
  public void swap() {
    // do nothing
  }
  
  // ======================================================================
  // == Generic manipulation
  // ======================================================================
 
  /** For exception handlers */
  public void exceptionHandler(final String type) {
    // do nothing
  }
  
  /** For finally handlers */
  public void finallyHandler() {
    // do nothing
  }
  
  public void pop() {
    // do nothing
  }
  
  public void pop2() {
    // do nothing
  }
  
  public void pop3() {
    // do nothing
  }
  
  public void pop4() {
    // do nothing
  }
  
  /**
   * Push a primitive.
   */
  public void pushPrimitive() {
    // do nothing
  }
  
  /**
   * Push a double wide (long or double) primitive.
   */
  public void pushPrimitive2() {
    // do nothing
  }  
  
  public void newarray(final String description) {
    // do nothing
  }
  
  /**
   * Handle loads from primitively typed arrays.
   */
  public void primitiveArrayLoad() {
    // do nothing
  }
  
  /**
   * Handle loads from long and double typed arrays.
   */
  public void primitiveArrayLoad2() {
    // do nothing
  }
  
  /**
   * Handle an instance method invocation that returns an object or array.
   * 
   * @param opcode
   *          The opcode of the method call.
   * @param argumentsSize
   *          The total size of all the method arguments.
   * @param owner
   *          The method's owner
   * @param name
   *          The method's name
   * @param description
   *          The method's description
   */
  public void invokeMethodReturnsObject(
      final int opcode, final int argumentsSize,
      final String owner, final String name, final String description) {
    // do nothing
  }
  
  /**
   * Handle an instance method invocation that doesn't return a value.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeMethodReturnsVoid(final int argumentsSize) {
    // do nothing
  }
  
  /**
   * Handle an instance method invocation that returns a primitive type.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeMethodReturnsPrimitive(final int argumentsSize) {
    // do nothing
  }

  /**
   * Handle an instance method invocation that returns a long or double.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeMethodReturnsPrimitive2(final int argumentsSize) {
    // do nothing
  }
  
  /**
   * Handle an static method invocation that returns an object or array.
   * 
   * @param opcode
   *          The opcode of the method call.
   * @param argumentsSize
   *          The total size of all the method arguments.
   * @param owner
   *          The method's owner
   * @param name
   *          The method's name
   * @param description
   *          The method's description
   */
  public void invokeStaticMethodReturnsObject(final int argumentsSize,
      final String owner, final String name, final String description) {
    // do nothing
  }
  
  /**
   * Handle an static method invocation that doesn't return a value.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeStaticMethodReturnsVoid(final int argumentsSize) {
    // do nothing
  }
  
  /**
   * Handle an static method invocation that returns a primitive type.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeStaticMethodReturnsPrimitive(final int argumentsSize) {
    // do nothing
  }
  
  /**
   * Handle an static method invocation that returns a long or double.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeStaticMethodReturnsPrimitive2(final int argumentsSize) {
    // do nothing
  }
}
