package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;
import java.util.Comparator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class WrapperMethod {
  private static final String WRAPPER_NAME_TEMPLATE = "flashlight${0}${1}$wrapper";
  private static final String WRAPPER_SIGNATURE_TEMPLATE = "(L{0};{1}Ljava/lang/String;I){2}";
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
    
  
  
  private final String wrapperName;
  private final String wrapperSignature;
  private final String owner;
  private final String originalName;
  private final String originalSignature;
  private final int opcode;
  
  private final String identityString;
  private final int hashCode;
  
  private final Type[] originalArgTypes;
  private final int[] wrapperArgsToLocals;
  private final int numWrapperLocals;
 
  private final Type originalReturnType;
  
  
  
  public WrapperMethod(final String owner, final String originalName,
      final String originalSignature, final int opcode) {
    this.wrapperName = createWrapperMethodName(owner, originalName);
    this.wrapperSignature = createWrapperMethodSignature(owner, originalSignature);
    this.owner = owner;
    this.originalName = originalName;
    this.originalSignature = originalSignature;
    this.opcode = opcode;
    
    this.identityString = owner + originalName + originalSignature;
    this.hashCode = identityString.hashCode();
    
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
  public boolean equals(final Object o) {
    if (o instanceof WrapperMethod) {
      return this.identityString.equals(((WrapperMethod) o).identityString);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
  


  private static String createWrapperMethodName(
      final String owner, final String name) {
    return MessageFormat.format(WRAPPER_NAME_TEMPLATE,
        owner.replace(INTERNAL_NAME_SEPARATOR, UNDERSCORE), name);
  }
  
  private static String createWrapperMethodSignature(
      final String owner, final String originalSignature) {
    final int endOfArgs = originalSignature.lastIndexOf(END_OF_ARGS);
    return MessageFormat.format(WRAPPER_SIGNATURE_TEMPLATE,
        owner, originalSignature.substring(1, endOfArgs),
        originalSignature.substring(endOfArgs + 1));
  }



  public String getWrapperName() {
    return wrapperName;
  }
  
  public String getWrapperSignature() {
    return wrapperSignature;
  }
  
  public int getNumLocals() {
    return numWrapperLocals;
  }
  
  
  
  public void pushObjectRef(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, wrapperArgsToLocals[OBJ_REF_ARG]);
  }
  
  public void pushCallingMethodName(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ALOAD, wrapperArgsToLocals[wrapperArgsToLocals.length - CALLING_METHOD_ARG_FROM_END]);
  }
  
  public void pushCallingLineNumber(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.ILOAD, wrapperArgsToLocals[wrapperArgsToLocals.length - CALLING_LINE_ARG_FROM_END]);
  }
  
  public void pushOriginalArguments(final MethodVisitor mv) {
    for (int i = 0; i < originalArgTypes.length; i++) {
      mv.visitVarInsn(originalArgTypes[i].getOpcode(Opcodes.ILOAD), wrapperArgsToLocals[FIRST_ORIGINAL_ARG + i]);
    }
  }
  
  public void invokeOriginalMethod(final MethodVisitor mv) {
    mv.visitMethodInsn(opcode, owner, originalName, originalSignature);
  }
  
  public void methodReturn(final MethodVisitor mv) {
    mv.visitInsn(originalReturnType.getOpcode(Opcodes.IRETURN));
  }
  
  public int getMethodReturnSize() {
    return originalReturnType.getSize();
  }
}
