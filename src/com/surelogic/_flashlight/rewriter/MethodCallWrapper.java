package com.surelogic._flashlight.rewriter;

import java.util.Comparator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

abstract class WrapperMethod {
  private static final char INTERNAL_NAME_SEPARATOR = '/';
  private static final char UNDERSCORE = '_';
  private static final char END_OF_ARGS = ')';

  private static final int OBJ_REF_ARG = 0;
  private static final int FIRST_ORIGINAL_ARG = 1;
  private static final int CALLING_METHOD_ARG_FROM_END = 2;
  private static final int CALLING_LINE_ARG_FROM_END = 1;
  
  public static final Comparator<WrapperMethod> comparator =
    new Comparator<WrapperMethod>() {
      public int compare(WrapperMethod o1, WrapperMethod o2) {
        return o1.identityString.compareTo(o2.identityString);
      }
    };
    
  
  
  protected final String wrapperName;
  protected final String wrapperSignature;
  protected final String owner;
  protected final String originalName;
  protected final String originalSignature;
  protected final int opcode;
  
  private final String identityString;
  private final int hashCode;
  
  protected final Type[] originalArgTypes;
  protected final int[] wrapperArgsToLocals;
  protected final int numWrapperLocals;
 
  protected final Type originalReturnType;
  
  
  
  public WrapperMethod(final String owner, final String originalName,
      final String originalSignature, final int opcode) {
    final String ownerUnderscored = owner.replace(INTERNAL_NAME_SEPARATOR, UNDERSCORE);
    final int endOfArgs = originalSignature.lastIndexOf(END_OF_ARGS);
    final String originalArgs = originalSignature.substring(1, endOfArgs);
    final String originalReturn = originalSignature.substring(endOfArgs + 1);

    this.wrapperName = createWrapperMethodName(ownerUnderscored, originalName, opcode);
    this.wrapperSignature = createWrapperMethodSignature(owner, originalArgs, originalReturn);
    this.owner = owner;
    this.originalName = originalName;
    this.originalSignature = originalSignature;
    this.opcode = opcode;
    
    this.identityString = owner + originalName + originalSignature + opcode;
    this.hashCode = identityString.hashCode();
    
    /* The mapping of arguments to locals is the same regardless of whether
     * the wrapper method is static or instance.  This is because the instance
     * method has an implicit first argument that is made explicit in our
     * static method.
     */
    originalArgTypes = Type.getArgumentTypes(originalSignature);
    wrapperArgsToLocals = new int[originalArgTypes.length+3]; // original plus objRef, method name, line number
    wrapperArgsToLocals[0] = 0;
    int nextArg = 1;
    int nextLocalVariable = 1;
    for (int i = 0; i < originalArgTypes.length; i++ ) {
      wrapperArgsToLocals[nextArg++] = nextLocalVariable;
      nextLocalVariable += originalArgTypes[i].getSize();
    }
    wrapperArgsToLocals[nextArg++] = nextLocalVariable++; // method name
    wrapperArgsToLocals[nextArg++] = nextLocalVariable++; // line number
    numWrapperLocals = nextLocalVariable;
    
    originalReturnType = Type.getReturnType(originalSignature);
  }
  
  @Override
  public final boolean equals(final Object o) {
    if (o instanceof WrapperMethod) {
      return this.identityString.equals(((WrapperMethod) o).identityString);
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  
  
  
  /**
   * Create the name of the wrapper method. Called from the constructor. This
   * should not look at any instance state of the class. It is only non-<code>static</code>
   * because <code>static</code> methods cannot be virtual.
   * 
   * @param ownerUnderscored
   *          The owner class of the wrapped method with '/' replaced by '_'.
   * @param name
   *          The simple name of the wrapped method.
   * @param opcode
   *          The opcode used to invoke the wrapped method.
   * @return The simple name of the wrapper method.
   */
  protected abstract String createWrapperMethodName(
      String owner, String name, int opcode);

  /**
   * Create the signature of the wrapper method. Called from the constructor.
   * This should not look at any instance state of the class. It is only non-<code>static</code>
   * because <code>static</code> methods cannot be virtual.
   * 
   * @param owner
   *          The owner class of the wrapped method.
   * @param originalArgs
   *          The argument types of the wrapped method, lifted from the wrapped
   *          method's signature.
   * @param originalReturn
   *          The return type of the wrapped method, lifted from the wrapped
   *          method's signature.
   * @return The signature of the wrapper method.
   */
  protected abstract String createWrapperMethodSignature(
      String owner, String originalArgs, String originalReturn);

  /**
   * Get the access flags for the wrapper method.
   * @return The access flags.
   */
  protected abstract int getAccess();



  public final int getNumLocals() {
    return numWrapperLocals;
  }
  
  
  
  public final MethodVisitor createMethodHeader(final ClassVisitor cv) {
    return cv.visitMethod(getAccess(), wrapperName, wrapperSignature, null, null);
  }
  
  public final void pushObjectRef(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, wrapperArgsToLocals[OBJ_REF_ARG]);
  }
  
  public final void pushCallingMethodName(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, wrapperArgsToLocals[wrapperArgsToLocals.length - CALLING_METHOD_ARG_FROM_END]);
  }
  
  public final void pushCallingLineNumber(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ILOAD, wrapperArgsToLocals[wrapperArgsToLocals.length - CALLING_LINE_ARG_FROM_END]);
  }
  
  public final void pushOriginalArguments(final MethodVisitor mv) {
    for (int i = 0; i < originalArgTypes.length; i++) {
      mv.visitVarInsn(originalArgTypes[i].getOpcode(Opcodes.ILOAD), wrapperArgsToLocals[FIRST_ORIGINAL_ARG + i]);
    }
  }
  
  public abstract void invokeWrapperMethod(
      MethodVisitor mv, String classBeingAnalyzed);
  
  public final void invokeOriginalMethod(final MethodVisitor mv) {
    mv.visitMethodInsn(opcode, owner, originalName, originalSignature);
  }
  
  public final void methodReturn(final MethodVisitor mv) {
    mv.visitInsn(originalReturnType.getOpcode(Opcodes.IRETURN));
  }
  
  public final int getMethodReturnSize() {
    return originalReturnType.getSize();
  }
}
