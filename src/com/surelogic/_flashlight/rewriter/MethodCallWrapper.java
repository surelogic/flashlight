package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;
import java.util.Comparator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

abstract class MethodCallWrapper {
  private static final String WRAPPER_NAME_TEMPLATE = "flashlight${0}${1}${2,choice,0#virtual|1#special|2#static|3#interface}Wrapper";
  private static final char INTERNAL_NAME_SEPARATOR = '/';
  private static final char UNDERSCORE = '_';
  private static final char END_OF_ARGS = ')';

  public static final Comparator<MethodCallWrapper> comparator =
    new Comparator<MethodCallWrapper>() {
      public int compare(MethodCallWrapper o1, MethodCallWrapper o2) {
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
  
  protected final int firstOriginalArgPos;
  protected final int callingMethodNamePos;
  protected final int callingLineNumberPos;
  
  protected final Type originalReturnType;
  
  
  
  public MethodCallWrapper(final String owner, final String originalName,
      final String originalSignature, final int opcode, final boolean isInstance) {
    final String ownerUnderscored = owner.replace(INTERNAL_NAME_SEPARATOR, UNDERSCORE);
    final int endOfArgs = originalSignature.lastIndexOf(END_OF_ARGS);
    final String originalArgs = originalSignature.substring(1, endOfArgs);
    final String originalReturn = originalSignature.substring(endOfArgs + 1);

    this.wrapperName = MessageFormat.format(WRAPPER_NAME_TEMPLATE,
        ownerUnderscored, originalName, (opcode - Opcodes.INVOKEVIRTUAL));
    this.wrapperSignature = createWrapperMethodSignature(owner, originalArgs, originalReturn);
    this.owner = owner;
    this.originalName = originalName;
    this.originalSignature = originalSignature;
    this.opcode = opcode;
    
    this.identityString = owner + originalName + originalSignature + opcode;
    this.hashCode = identityString.hashCode();
    
    this.originalArgTypes = Type.getArgumentTypes(originalSignature);

    final Type[] wrapperArgTypes = Type.getArgumentTypes(wrapperSignature);
    wrapperArgsToLocals = new int[wrapperArgTypes.length];
    int nextLocalVariable = isInstance ? 1 : 0;
    for (int i = 0; i < wrapperArgTypes.length; i++ ) {
      wrapperArgsToLocals[i] = nextLocalVariable;
      nextLocalVariable += wrapperArgTypes[i].getSize();
    }
    numWrapperLocals = nextLocalVariable;
    
    firstOriginalArgPos = getFirstOriginalArgPosition(originalArgTypes.length);
    callingMethodNamePos = getCallingMethodNamePosition(originalArgTypes.length);
    callingLineNumberPos = getCallingLineNumberPosition(originalArgTypes.length);
    
    originalReturnType = Type.getReturnType(originalSignature);
  }
  
  @Override
  public final boolean equals(final Object o) {
    if (o instanceof MethodCallWrapper) {
      return this.identityString.equals(((MethodCallWrapper) o).identityString);
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  
  
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

  protected abstract int getFirstOriginalArgPosition(int numOriginalArgs);
  
  protected abstract int getCallingMethodNamePosition(int numOriginalArgs);
  
  protected abstract int getCallingLineNumberPosition(int numOriginalArgs);
  

  
  public final boolean testOriginalName(final String testOwner, final String testName) {
    return owner.equals(testOwner) && originalName.equals(testName);
  }
  
  public final boolean testOriginalSignature(final String testSignature) {
    return originalSignature.equals(testSignature);
  }

  
  
  public final int getNumLocals() {
    return numWrapperLocals;
  }
  
  
  
  public final MethodVisitor createMethodHeader(final ClassVisitor cv) {
    return cv.visitMethod(getAccess(), wrapperName, wrapperSignature, null, null);
  }
  
  public abstract void pushObjectRefForEvent(MethodVisitor mv);

  public abstract void pushObjectRefForOriginalMethod(MethodVisitor mv);
  
  public final void pushCallingMethodName(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, wrapperArgsToLocals[callingMethodNamePos]);
  }
  
  public final void pushCallingLineNumber(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ILOAD, wrapperArgsToLocals[callingLineNumberPos]);
  }
  
  public final void pushOriginalArguments(final MethodVisitor mv) {
    for (int i = 0; i < originalArgTypes.length; i++) {
      mv.visitVarInsn(originalArgTypes[i].getOpcode(Opcodes.ILOAD),
          wrapperArgsToLocals[firstOriginalArgPos + i]);
    }
  }
  
  public final void invokeWrapperMethod(
      final MethodVisitor mv, final String classBeingAnalyzed) {
    mv.visitMethodInsn(getWrapperMethodOpcode(), classBeingAnalyzed,
        wrapperName, wrapperSignature);
  }
  
  protected abstract int getWrapperMethodOpcode();
  
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
