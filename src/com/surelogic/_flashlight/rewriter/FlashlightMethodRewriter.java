package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class FlashlightMethodRewriter extends MethodAdapter {
  // Constants for accessing the special Flashlight Store class
  private static final String FLASHLIGHT_STORE = "com/surelogic/_flashlight/rewriter/test/DebugStore";
  private static final String STORE_FIELD_ACCESS = "fieldAccess";
  private static final String FIELD_ACCESS_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/reflect/Field;Ljava/lang/Class;I)V";

  // Other Java classes and methods
  private static final String CONSTRUCTOR = "<init>";
  
  private static final String JAVA_LANG_CLASS = "java/lang/Class";
  private static final String CLASS_FOR_NAME = "forName";
  private static final String FOR_NAME_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";
  private static final String CLASS_GET_DECLARED_FIELD = "getDeclaredField";
  private static final String GET_DECLARED_FIELD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/reflect/Field;";

  private static final String JAVA_LAND_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";

  private static final String JAVA_LANG_ERROR = "java/lang/Error";
  private static final String ERROR_SIGNATURE = "(Ljava/lang/String;)V";

  private static final String JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";


  
  /** The fully qualified name of the class being rewritten */
  private final String classBeingAnalyzed;
  
  /**
   * The current source line of code being rewritten. Driven by calls to
   * {@link #visitLineNumber}. This is {@code -1} when no line number
   * information is available.
   */
  private Integer currentSrcLine = Integer.valueOf(-1);
  
  /**
   * The amount by which the stack depth must be increased.
   */
  private int stackDepthDelta = 0;
  
  
  
  /**
   * Create a new method rewriter.
   * 
   * @param className
   *          The fully qualified name of the class being rewritten.
   * @param mv
   *          The {@code MethodVisitor} to delegate to.
   */
  public FlashlightMethodRewriter(
      final String className, final MethodVisitor mv) {
    super(mv);
    classBeingAnalyzed = className;
  }
  
  
  
  @Override
  public void visitLineNumber(final int line, final Label start) {
    mv.visitLineNumber(line, start);
    currentSrcLine = Integer.valueOf(line);
  }
  
  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    if (opcode == Opcodes.PUTFIELD) {
      rewritePutfield(owner, name, desc);
    } else if (opcode == Opcodes.PUTSTATIC) {
      rewritePutstatic(owner, name, desc);
    } else if (opcode == Opcodes.GETFIELD) {
      rewriteGetfield(owner, name, desc);
    } else if (opcode == Opcodes.GETSTATIC) {
      rewriteGetstatic(owner, name, desc);
    } else {
      mv.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  
  
  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    mv.visitMaxs(maxStack + stackDepthDelta, maxLocals);
  }



  /**
   * Update the stack depth delta. The stack depth must be increased by at least
   * as much as the value provided here. If {@code newDelta} is less than the
   * current stack depth delta then we do nothing. Otherwise we update delta;
   * 
   * @param newDelta
   *          The minimum amount by which the stack depth must be increased.
   */
  private void updateStackDepthDelta(final int newDelta) {
    if (newDelta > stackDepthDelta) {
      stackDepthDelta = newDelta;
    } 
  }
  
  
  
  /**
   * Rewrite a {@code PUTFIELD} instruction.
   * 
   * @param owner
   *          the internal name of the field's owner class (see {@link
   *          Type#getInternalName() getInternalName}).
   * @param name
   *          the field's name.
   * @param desc
   *          the field's descriptor (see {@link Type Type}).
   */
  private void rewritePutfield(
      final String owner, final String name, final String desc) {
    final String fullyQualifiedOwner = Utils.internal2FullyQualified(owner);
    final int stackDelta;
    
    /* We need to manipulate the stack to make a copy of the object being
     * accessed so that we can have it for the call to the Store.
     * How we do this depends on whether the top value on the stack is a
     * catagory 1 or a category 2 value.  We have to test the type descriptor
     * of the field to determine this.
     */
    if (Utils.isCategory2(desc)) {
      // Category 2
      stackDelta = 2;
      
      // At the start the stack is "..., objectref, value"
      mv.visitInsn(Opcodes.DUP2_X1);
      // Stack is "..., value, objectref, value"
      mv.visitInsn(Opcodes.POP2);
      // Stack is "..., value, objectref"
      mv.visitInsn(Opcodes.DUP_X2);
      // Stack is "..., objectref, value, objectref"
      mv.visitInsn(Opcodes.DUP_X2);
      // Stack is "..., objectref, objectref, value, objectref"
      mv.visitInsn(Opcodes.POP);
      // Stack is "..., objectref, objectref, value"      
    } else {
      // Category 1
      stackDelta = 3;
  
      // At the start the stack is "..., objectref, value"
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "..., value, objectref"
      mv.visitInsn(Opcodes.DUP_X1);
      // Stack is "..., objectref, value, objectref"
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "..., objectref, objectref, value"
    }
    
    // Execute the original PUTFIELD instruction
    mv.visitFieldInsn(Opcodes.PUTFIELD, owner, name, desc);
    // Stack is "..., objectref"
    
    /* Again manipulate the stack so that we can set up the first two
     * arguments to the Store.fieldAccess() call.  The first argument
     * is a boolean "isRead" flag.  The second argument is the object being
     * accessed.
     */
    mv.visitInsn(Opcodes.ICONST_0); // Push "false"
    // Stack is "..., objectref, 0"
    mv.visitInsn(Opcodes.SWAP);
    // Stack is "..., 0, objectref"
    
    finishFieldAccess(name, fullyQualifiedOwner);
    
    // Update stack depth
    updateStackDepthDelta(stackDelta);
  }



  /**
   * Rewrite a {@code GETFIELD} instruction.
   * 
   * @param owner
   *          the internal name of the field's owner class (see {@link
   *          Type#getInternalName() getInternalName}).
   * @param name
   *          the field's name.
   * @param desc
   *          the field's descriptor (see {@link Type Type}).
   */
  private void rewriteGetfield(
      final String owner, final String name, final String desc) {
    final String fullyQualifiedOwner = Utils.internal2FullyQualified(owner);
    // Stack is "..., objectref"
    
    /* We need to manipulate the stack to make a copy of the object being
     * accessed so that we can have it for the call to the Store.
     */
    mv.visitInsn(Opcodes.DUP);
    // Stack is "..., objectref, objectref"
    
    // Execute the original GETFIELD instruction
    mv.visitFieldInsn(Opcodes.GETFIELD, owner, name, desc);
    // Stack is "..., objectref, value"   [Value could be cat1 or cat2!]
    
    /* Again manipulate the stack so that we push the value below the objectref
     * so that we have the objectref for the call to Store.fieldAccess().  Also
     * need to insert "true" for the "isRead" parameter to fieldAccess().  How
     * we do this depends on whether the value is category1 or category2.
     */
    if (Utils.isCategory2(desc)) {
      // Category 2
      mv.visitInsn(Opcodes.DUP2_X1);
      // Stack is "..., value, objectref, value"
      mv.visitInsn(Opcodes.POP2);
      // Stack is "..., value, objectref"
    } else {
      // Category 1
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "..., value, objectref"
    }
    mv.visitInsn(Opcodes.ICONST_1); // Push "true"
    // Stack is "..., value, objectref, 1"
    mv.visitInsn(Opcodes.SWAP);
    // Stack is "..., value, 1, objectref"
    
    finishFieldAccess(name, fullyQualifiedOwner);
    
    // Update stack depth
    updateStackDepthDelta(5);
  }



  /**
   * Rewrite a {@code PUTSTATIC} instruction.
   * 
   * @param owner
   *          the internal name of the field's owner class (see {@link
   *          Type#getInternalName() getInternalName}).
   * @param name
   *          the field's name.
   * @param desc
   *          the field's descriptor (see {@link Type Type}).
   */
  private void rewritePutstatic(
      final String owner, final String name, final String desc) {
    final String fullyQualifiedOwner = Utils.internal2FullyQualified(owner);
    
    // Stack is "..., value"
    
    // Execute the original PUTSTATIC instruction
    mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, desc);
    // Stack is "..."
    
    /* Push the first arguments on the stack for the call to
     * Store.fieldAccess().  The first argument is a boolean "isRead" flag.
     * The second argument is the object being accessed, which is "null"
     * in this case.
     */
    mv.visitInsn(Opcodes.ICONST_0); // Push "false"
    // Stack is "..., 0"
    mv.visitInsn(Opcodes.ACONST_NULL);
    // Stack is "..., 0, null"
    
    finishFieldAccess(name, fullyQualifiedOwner);
    
    /* Update stack depth.  Either 3 or 4 depending on the category of the
     * original value on the stack.
     */
    updateStackDepthDelta(Utils.isCategory2(desc) ? 3 : 4);
  }
  
  /**
   * Rewrite a {@code GETSTATIC} instruction.
   * 
   * @param owner
   *          the internal name of the field's owner class (see {@link
   *          Type#getInternalName() getInternalName}).
   * @param name
   *          the field's name.
   * @param desc
   *          the field's descriptor (see {@link Type Type}).
   */
  private void rewriteGetstatic(
      final String owner, final String name, final String desc) {
    final String fullyQualifiedOwner = Utils.internal2FullyQualified(owner);
    // Stack is "..."
    
    // Execute the original GETFIELD instruction
    mv.visitFieldInsn(Opcodes.GETSTATIC, owner, name, desc);
    // Stack is "..., value"   [Value could be cat1 or cat2!]
    
    /* Manipulate the stack so that we push the first two arguments to 
     * Store.fieldAccess().
     */
    mv.visitInsn(Opcodes.ICONST_1); // Push "true"
    // Stack is "..., value, 1"
    mv.visitInsn(Opcodes.ACONST_NULL);
    // Stack is "..., value, 1, null"
    
    finishFieldAccess(name, fullyQualifiedOwner);
    
    // Update stack depth
    updateStackDepthDelta(5);
  }



  /**
   * All the field access rewrites end the same way once the first two
   * parameters of Store.fieldAccess() are placed on the stack.  This
   * pushes the rest of the parameters on the stack and introduces the
   * call to Store.fieldAccess().  Adds the necessary exception handlers.
   * 
   * <p>
   * The JVM stack needs to be "..., <i>isRead</i>, <i>receiver</i>" when this
   * method is called.
   * 
   * @param name
   *          The name of the field being accessed.
   * @param fullyQualifiedOwner
   *          The fully qualified class name of the class that declares the
   *          field being accessed.
   */
  private void finishFieldAccess(
      final String name, final String fullyQualifiedOwner) {
    // Stack is "..., isRead, receiver"
    
    /* We have to create try-catch blocks to deal with the exceptions that
     * the inserted reflection methods might throw.  We do this on a per-call
     * basis here in the bytecode, nested within the creation of the argument
     * list for the ultimate call to Store.fieldAccess().  This would not be
     * possible in the Java source code unless we used local variables.  But
     * the bytecode is more flexible.
     */
    final Label try1Start = new Label();
    final Label try1End_try2Start = new Label();
    final Label try2End_try3Start = new Label();
    final Label try3End = new Label();
    final Label catchClassNotFound1 = new Label();
    final Label catchNoSuchField = new Label();
    final Label catchClassNotFound2 = new Label();
    mv.visitTryCatchBlock(try1Start, try1End_try2Start, catchClassNotFound1, JAVA_LAND_CLASS_NOT_FOUND_EXCEPTION);
    mv.visitTryCatchBlock(try1End_try2Start, try2End_try3Start, catchNoSuchField, JAVA_LANG_NO_SUCH_FIELD_EXCEPTION);
    mv.visitTryCatchBlock(try2End_try3Start, try3End, catchClassNotFound2, JAVA_LAND_CLASS_NOT_FOUND_EXCEPTION);
    
    /* We need to insert the expression
     * "Class.forName(<owner>).getDeclaredField(<name>)" into the code.  This puts
     * the java.lang.reflect.Field object for the accessed field on the stack.
     * We have a try-catch for the call to forName() and a separate try-catch
     * for the call to getDeclaredField().
     */
    mv.visitLabel(try1Start);
    mv.visitLdcInsn(fullyQualifiedOwner);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, JAVA_LANG_CLASS, CLASS_FOR_NAME, FOR_NAME_SIGNATURE);
    mv.visitLabel(try1End_try2Start);
    mv.visitLdcInsn(name);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JAVA_LANG_CLASS, CLASS_GET_DECLARED_FIELD, GET_DECLARED_FIELD_SIGNATURE);
    mv.visitLabel(try2End_try3Start);
    // Stack is "..., isRead, receiver, Field"
    
    /* We need to insert the expression "Class.forName(<current_class>)"
     * to push the java.lang.Class object of the referencing class onto the 
     * stack.
     */
    mv.visitLdcInsn(classBeingAnalyzed);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, JAVA_LANG_CLASS, CLASS_FOR_NAME, FOR_NAME_SIGNATURE);
    mv.visitLabel(try3End);
    // Stack is "..., isRead, receiver, Field, Class"
    
    /* We need to push the line number of the field access.  We could be smart
     * about how we do this based on whether the line number fits into an 8-,
     * 16-, or 32-bit value.  Right now we are dumb.
     */
    mv.visitLdcInsn(currentSrcLine);
    // Stack is "..., isRead, receiver, Field, Class, LineNumber"
    
    /* We can now call Store.fieldAccess() */
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE, STORE_FIELD_ACCESS, FIELD_ACCESS_SIGNATURE);
    
    // Stack is "..."
    
    /* Insert catch blocks.  We also have to insert a jump around them. */
    final Label noExceptions = new Label();
    mv.visitJumpInsn(Opcodes.GOTO, noExceptions);
    
    mv.visitLabel(catchClassNotFound1); // catch ClassNotFoundException
    mv.visitTypeInsn(Opcodes.NEW, JAVA_LANG_ERROR);
    mv.visitInsn(Opcodes.DUP);
    mv.visitLdcInsn("Failed to find Class object for " + fullyQualifiedOwner);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, JAVA_LANG_ERROR, CONSTRUCTOR, ERROR_SIGNATURE);
    mv.visitInsn(Opcodes.ATHROW);
  
    mv.visitLabel(catchClassNotFound2); // catch ClassNotFoundException
    mv.visitTypeInsn(Opcodes.NEW, JAVA_LANG_ERROR);
    mv.visitInsn(Opcodes.DUP);
    mv.visitLdcInsn("Failed to find Class object for " + classBeingAnalyzed);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, JAVA_LANG_ERROR, CONSTRUCTOR, ERROR_SIGNATURE);
    mv.visitInsn(Opcodes.ATHROW);
  
    mv.visitLabel(catchNoSuchField); // catch NoSuchFieldException
    mv.visitTypeInsn(Opcodes.NEW, JAVA_LANG_ERROR);
    mv.visitInsn(Opcodes.DUP);
    mv.visitLdcInsn("Failed to Field object for " + name + " in class " + fullyQualifiedOwner);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, JAVA_LANG_ERROR, CONSTRUCTOR, ERROR_SIGNATURE);
    mv.visitInsn(Opcodes.ATHROW);
  
    /* Resume original instruction stream */
    mv.visitLabel(noExceptions);
  }
}
