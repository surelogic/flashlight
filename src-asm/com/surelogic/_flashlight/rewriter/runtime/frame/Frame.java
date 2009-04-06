package com.surelogic._flashlight.rewriter.runtime.frame;

public final class Frame {
  private static final String EOL = System.getProperty("line.separator");
  
  
  
  /**
   * The identity of the local variables. Array elements must never be
   * {@code null}; use {@link LocalVariable#UNKNOWN} if the id is not known.
   */
  private final LocalVariable[] locals;

  /**
   * The values of the local variables.
   */
  private final StackItem[] localValues;
  
  /**
   * The stack.  Grows upwards from the zero index.
   */
  private final StackItem[] stack;
  
  /**
   * The index of the top element of the stack, or -1 if the stack is empty.
   * Add elements by preincrement.  Remove by postdecrement.
   */
  private int topOfStack;
  
  /**
   * The current line number, or {@value -1} if the current line number is
   * unknown. We maintain this as state here, instead of taking it as a
   * parameter for each stack value because it is easier to set the line number
   * as the method visitor receives it via the
   * {@link org.objectweb.asm.MethodAdapter#visitLineNumber(int, org.objectweb.asm.Label)}
   * callback. This also means less work needs to done for each stack
   * manipulation at runtime, and should thus decrease the total amount of
   * overhead dedicated to modeling the stack.
   */
  private int currentSourceLine;
  
  
  
  public Frame(final int numLocals, final int stackSize) {
    locals = new LocalVariable[numLocals];
    localValues = new StackItem[numLocals];
    stack = new StackItem[stackSize];
    topOfStack = -1;
    currentSourceLine = -1;
    
    // Init local variables
    for (int i = 0; i < numLocals; i++) {
      locals[i] = LocalVariable.UNKNOWN;
    }
  }
  
  
  
  /**
   * Forget the identity of a local variable.
   */
  public void clearLocalVariable(final int index) {
    locals[index] = LocalVariable.UNKNOWN;
  }
  
  /**
   * Set (or reset) the identity of a local variable.
   */
  public void setLocalVariable(
      final int index, final String name, final String description) {
    locals[index] = LocalVariable.create(name, description);
  }
  
  /**
   * Initialize the receiver. 
   */
  public void initReceiver() {
    localValues[0] = FromReceiver.PROTOTYPE;
  }
  
  
  /**
   * Initialize a local variable holding a parameter value.
   */
  public void initParameter(final int localIdx, final int argIdx) {
    localValues[localIdx] = new FromParameter(argIdx);
  }
  
  
  
  /**
   * Set the current line number.
   */
  public void setCurrentSourceLine(final int line) {
    currentSourceLine = line;
  }

  
  
  public String dumpLocals() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < locals.length; i++) {
      sb.append(i);
      sb.append(": ");
      sb.append(locals[i]);
      sb.append(" = ");
      sb.append(localValues[i]);
      sb.append(EOL);
    }    
    return sb.toString();
  }
  
  public String dumpStack() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i <= topOfStack; i++) {
      sb.append(i);
      sb.append(": ");
      sb.append(stack[i]);
      sb.append(EOL);
    }    
    return sb.toString();
  }
  
  public String dump() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Current line = ");
    sb.append(currentSourceLine);
    sb.append(EOL);
    sb.append("Locals:");
    sb.append(EOL);
    sb.append(dumpLocals());
    sb.append("Stack:");
    sb.append(EOL);
    sb.append(dumpStack());
    return sb.toString();
  }
  
  
  
  // ======================================================================
  // == Handle specific opcodes
  // ======================================================================
  
  public void aaload() {
    topOfStack -= 1;
    final StackItem array = stack[topOfStack];
    // Ignore array index for now
    stack[topOfStack] = new FromArrayReference(currentSourceLine, array, -1);
  }
  
  public void aload(final int localIdx) {
    stack[++topOfStack] = new FromLocalVariable(
        currentSourceLine, locals[localIdx], localValues[localIdx]);
  }
  
  public void arraylength() {
    /* convert the top element to a primitive */
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
  }

  public void astore(final int localIdx) {
    localValues[localIdx] = stack[topOfStack--];
  }
  
  public void athrow() {
    /* Take the top element and make it the first element of a one element
     * stack.
     */
    stack[0] = stack[topOfStack];
    topOfStack = 0;
  }
  
  public void dup() {
    final int newTop = topOfStack + 1;
    stack[newTop] = stack[topOfStack];
    topOfStack = newTop;
  }
  
  public void dup_x1() {
    final StackItem value2 = stack[topOfStack-1];
    final StackItem value1 = stack[topOfStack];
    stack[topOfStack-1] = value1;
    stack[topOfStack] = value2;
    stack[++topOfStack] = value1;
  }

  public void dup_x2() {
    final StackItem value3 = stack[topOfStack-2];
    final StackItem value2 = stack[topOfStack-1];
    final StackItem value1 = stack[topOfStack];
    stack[topOfStack-2] = value1;
    stack[topOfStack-1] = value3;
    stack[topOfStack] = value2;
    stack[++topOfStack] = value1;
  }

  public void dup2() {
    final StackItem value2 = stack[topOfStack-1];
    final StackItem value1 = stack[topOfStack];
    stack[++topOfStack] = value2;
    stack[++topOfStack] = value1;
  }
  
  public void dup2_x1() {
    final StackItem value3 = stack[topOfStack-2];
    final StackItem value2 = stack[topOfStack-1];
    final StackItem value1 = stack[topOfStack];
    stack[topOfStack-2] = value2;
    stack[topOfStack-1] = value1;
    stack[topOfStack] = value3;
    stack[++topOfStack] = value2;
    stack[++topOfStack] = value1;
  }

  public void dup2_x2() {
    final StackItem value4 = stack[topOfStack-3];
    final StackItem value3 = stack[topOfStack-2];
    final StackItem value2 = stack[topOfStack-1];
    final StackItem value1 = stack[topOfStack];
    stack[topOfStack-3] = value2;
    stack[topOfStack-2] = value1;
    stack[topOfStack-1] = value4;
    stack[topOfStack] = value3;
    stack[++topOfStack] = value2;
    stack[++topOfStack] = value1;
  }

  public void getfieldObject(
      final String owner, final String name, final String description) {
    stack[topOfStack] = new FromFieldReference(
        currentSourceLine, stack[topOfStack],
        new OwnedName(owner, name, description));
  }

  public void getfieldPrimitive() {
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
  }

  public void getfieldPrimitive2() {
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }

  public void getstaticObject(
      final String owner, final String name, final String description) {
    stack[++topOfStack] = new FromStaticFieldReference(currentSourceLine,
        new OwnedName(owner, name, description));
  }
  
  public void getstaticPrimitive() {
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }
  
  public void getstaticPrimitive2() {
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }
  
  public void instanceOf() {
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
  }
  
  public void ldcString(final String value) {
    stack[++topOfStack] = new FromStringConstant(currentSourceLine, value);
  }
  
  public void ldcClass(final String classname) {
    stack[++topOfStack] = new FromClassConstant(currentSourceLine, classname);
  }
  
  /**
   * @param elementType The type of the array elements.  Must be derived from
   *  the type in the raw instruction.
   * @param dims The number of dimensions on the stack
   */
  public void multianewarray(final String elementType, final int dims) {
    topOfStack -= dims;
    stack[++topOfStack] = new FromNewArray(currentSourceLine, elementType);
  }
  
  public void newObject(final String description) {
    stack[++topOfStack] = new FromNewObject(currentSourceLine, description);
  }
  
  public void swap() {
    final StackItem temp = stack[topOfStack];
    stack[topOfStack] = stack[topOfStack-1];
    stack[topOfStack-1] = temp;
  }
  
  // ======================================================================
  // == Generic manipulation
  // ======================================================================
 
  /** For exception handlers */
  public void exceptionHandler(final String type) {
    topOfStack = 0;
    stack[0] = new FromException(currentSourceLine, type);
  }
  
  /** For finally handlers */
  public void finallyHandler() {
    topOfStack = 0;
    stack[0] = new FromException(currentSourceLine, "Ljava/lang/Throwable;");
  }
  
  public void pop() {
    topOfStack -= 1;
  }
  
  public void pop2() {
    topOfStack -= 2;
  }
  
  public void pop3() {
    topOfStack -= 3;
  }
  
  public void pop4() {
    topOfStack -= 4;
  }
  
  /**
   * Push a primitive.
   */
  public void pushPrimitive() {
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }
  
  /**
   * Push a double wide (long or double) primitive.
   */
  public void pushPrimitive2() {
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }  
  
  public void newarray(final String description) {
    stack[topOfStack] = new FromNewArray(currentSourceLine, description);
  }
  
  /**
   * Handle loads from primitively typed arrays.
   */
  public void primitiveArrayLoad() {
    topOfStack -= 1;
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
  }
  
  /**
   * Handle loads from long and double typed arrays.
   */
  public void primitiveArrayLoad2() {
    stack[topOfStack-1] = IsPrimitive.PROTOTYPE;
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
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
//    System.out.println(dump());
    // pop args
    topOfStack -= argumentsSize;
    // replace receiver with return value
    stack[topOfStack] = new FromMethodCall(
        currentSourceLine, opcode, stack[topOfStack],
        new OwnedName(owner, name, description));
  }
  
  /**
   * Handle an instance method invocation that doesn't return a value.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeMethodReturnsVoid(final int argumentsSize) {
    topOfStack -= argumentsSize; // pop arguments
    topOfStack -= 1; // pop receiver
  }
  
  /**
   * Handle an instance method invocation that returns a primitive type.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeMethodReturnsPrimitive(final int argumentsSize) {
    topOfStack -= argumentsSize;
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
  }

  /**
   * Handle an instance method invocation that returns a long or double.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeMethodReturnsPrimitive2(final int argumentsSize) {
    topOfStack -= argumentsSize;
    stack[topOfStack] = IsPrimitive.PROTOTYPE;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
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
    // pop args
    topOfStack -= argumentsSize;
    // push return value
    stack[++topOfStack] = new FromStaticMethodCall(
        currentSourceLine, new OwnedName(owner, name, description));
  }
  
  /**
   * Handle an static method invocation that doesn't return a value.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeStaticMethodReturnsVoid(final int argumentsSize) {
    topOfStack -= argumentsSize;
  }
  
  /**
   * Handle an static method invocation that returns a primitive type.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeStaticMethodReturnsPrimitive(final int argumentsSize) {
    topOfStack -= argumentsSize;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }
  
  /**
   * Handle an static method invocation that returns a long or double.
   * 
   * @param argumentsSize
   *          The total size of all the method arguments.
   */
  public void invokeStaticMethodReturnsPrimitive2(final int argumentsSize) {
    topOfStack -= argumentsSize;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
    stack[++topOfStack] = IsPrimitive.PROTOTYPE;
  }
}
