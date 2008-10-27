package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;
import java.util.Comparator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Abstract representation of the wrapper method that is generated to replace
 * method calls. Primarily encapsulates differences in whether the method is
 * static or not, and differences in parameters.
 */
abstract class MethodCallWrapper extends MethodCall {
  private static final String WRAPPER_NAME_TEMPLATE_DEFAULT = "flashlight${0}${1}${2,choice,0#virtual|1#special|2#static|3#interface}Wrapper";
  private static final String WRAPPER_NAME_TEMPLATE_RECEIVER = "flashlight${0}${1}${2}{3,choice,0#virtual|1#special|2#static|3#interface}Wrapper";
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
  protected final String wrapperDescriptor;
  
  private final String identityString;
  private final int hashCode;
  
  protected final Type[] originalArgTypes;
  protected final int[] wrapperArgsToLocals;
  protected final int numWrapperLocals;
  
  protected final int firstOriginalArgPos;
  protected final int siteIdArgPos;
  protected final Type originalReturnType;
  
  
  
  /**
   * 
   * @param opcode
   *          The opcode used to invoke the original method.
   * @param rcvrTypeInternal
   *          If non-null this is the type of the receiver of the method being
   *          wrapped. If non-null this replaces the use of owner for the
   *          receiver type. This is necessary due to some stupidity in the
   *          Eclipse compiler and how it produces nested class access
   *          methods.
   * @param owner
   *          The owner of the original method.
   * @param originalName
   *          The name of the original method.
   * @param originalDesc
   *          The descriptor of the original method.
   * @param isInstance
   *          Should the wrapper method be an instance method?
   */
  public MethodCallWrapper(final int opcode,
      final String rcvrTypeInternal, final String owner,
      final String originalName, final String originalDesc,
      final boolean isInstance) {
    super(opcode, owner, originalName, originalDesc);
    final String ownerUnderscored = fixOwnerNameForMethodName(owner);
    final int endOfArgs = originalDesc.lastIndexOf(END_OF_ARGS);
    final String originalArgs = originalDesc.substring(1, endOfArgs);
    final String originalReturn = originalDesc.substring(endOfArgs + 1);

    if (rcvrTypeInternal == null) {
      this.wrapperName = MessageFormat.format(WRAPPER_NAME_TEMPLATE_DEFAULT,
          ownerUnderscored, originalName, (opcode - Opcodes.INVOKEVIRTUAL));
      this.wrapperDescriptor = createWrapperMethodSignature(
          fixOwnerNameForDescriptor(owner), originalArgs, originalReturn);
      
      this.identityString = owner + originalName + originalDesc + opcode;
    } else {
      this.wrapperName = MessageFormat.format(WRAPPER_NAME_TEMPLATE_RECEIVER,
          fixOwnerNameForMethodName(rcvrTypeInternal), ownerUnderscored,
          originalName, (opcode - Opcodes.INVOKEVIRTUAL));
      this.wrapperDescriptor = createWrapperMethodSignature(
          fixOwnerNameForDescriptor(rcvrTypeInternal), originalArgs, originalReturn);
      this.identityString = rcvrTypeInternal + owner + originalName + originalDesc + opcode;
    }
    this.hashCode = identityString.hashCode();
    
    this.originalArgTypes = Type.getArgumentTypes(originalDesc);

    final Type[] wrapperArgTypes = Type.getArgumentTypes(wrapperDescriptor);
    wrapperArgsToLocals = new int[wrapperArgTypes.length];
    int nextLocalVariable = isInstance ? 1 : 0;
    for (int i = 0; i < wrapperArgTypes.length; i++ ) {
      wrapperArgsToLocals[i] = nextLocalVariable;
      nextLocalVariable += wrapperArgTypes[i].getSize();
    }
    numWrapperLocals = nextLocalVariable;
    
    firstOriginalArgPos = getFirstOriginalArgPosition(originalArgTypes.length);
    siteIdArgPos = getSiteIdArgPosition(originalArgTypes.length);
    
    originalReturnType = Type.getReturnType(originalDesc);
  }
  
  /**
   * Fix the owner name that is suitable to use in the generated method name.
   * Converts and "/" to "_".  We also have to handle the crazy case of the 
   * owner being an array, which can happen in the case of the "clone" method.
   * In that case, we turn each leading "[" into "arrayOf_".  We also expand out
   * any of the single character descriptors for primitive types, and discard 
   * the "L" and ";" around class names.
   */
  private static String fixOwnerNameForMethodName(final String inOwner) {
    if (inOwner.charAt(0) == '[') {
      final StringBuilder sb = new StringBuilder();
      int index = 0;
      while (inOwner.charAt(index) == '[') {
        sb.append("arrayOf_");
        index += 1;
      }
      final char type = inOwner.charAt(index);
      if (type == 'B') { sb.append("byte"); }
      else if (type == 'C') { sb.append("char"); }
      else if (type == 'D') { sb.append("double"); }
      else if (type == 'F') { sb.append("float"); }
      else if (type == 'I') { sb.append("int"); }
      else if (type == 'J') { sb.append("long"); }
      else if (type == 'L') {
        sb.append(inOwner.substring(index + 1, inOwner.length() - 1).replace(
            INTERNAL_NAME_SEPARATOR, UNDERSCORE));
      } else if (type == 'S') { sb.append("short"); }
      else if (type == 'Z') { sb.append("boolean"); }
      return sb.toString();
    } else {
      return inOwner.replace(INTERNAL_NAME_SEPARATOR, UNDERSCORE);
    }
  }  
  
  private static String fixOwnerNameForDescriptor(final String inOwner) {
    if (inOwner.charAt(0) == '[') {
      return inOwner;
    } else {
      return "L" + inOwner + ";";
    }
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
  
  protected abstract int getSiteIdArgPosition(int numOriginalArgs);
  
  protected abstract void pushReceiverForOriginalMethod(MethodVisitor mv);

  
  public final int getNumLocals() {
    return numWrapperLocals;
  }
  
  
  
  public final MethodVisitor createMethodHeader(final ClassVisitor cv) {
    return cv.visitMethod(getAccess(), wrapperName, wrapperDescriptor, null, null);
  }
  
  @Override
  public final void pushSiteId(final MethodVisitor mv) {
    mv.visitVarInsn(Opcodes.LLOAD, wrapperArgsToLocals[siteIdArgPos]);
  }
  
  @Override
  public final void pushReceiverAndArguments(final MethodVisitor mv) {
    pushReceiverForOriginalMethod(mv);
    for (int i = 0; i < originalArgTypes.length; i++) {
      mv.visitVarInsn(originalArgTypes[i].getOpcode(Opcodes.ILOAD),
          wrapperArgsToLocals[firstOriginalArgPos + i]);
    }
  }
  
  public final void invokeWrapperMethod(
      final MethodVisitor mv, final String classBeingAnalyzed) {
    mv.visitMethodInsn(getWrapperMethodOpcode(), classBeingAnalyzed,
        wrapperName, wrapperDescriptor);
  }
  
  protected abstract int getWrapperMethodOpcode();
  
  public final void methodReturn(final MethodVisitor mv) {
    mv.visitInsn(originalReturnType.getOpcode(Opcodes.IRETURN));
  }
  
  public final int getMethodReturnSize() {
    return originalReturnType.getSize();
  }
}
