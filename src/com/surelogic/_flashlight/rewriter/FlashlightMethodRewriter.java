package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class FlashlightMethodRewriter extends MethodAdapter {
  // Constants for accessing the special Flashlight Store class
  private static final String FLASHLIGHT_STORE = "com/surelogic/_flashlight/rewriter/test/DebugStore";
  private static final String AFTER_INTRINSIC_LOCK_ACQUISITION = "afterIntrinsicLockAcquisition";
  private static final String AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Class;I)V";
  private static final String AFTER_INTRINSIC_LOCK_RELEASE = "afterIntrinsicLockRelease";
  private static final String AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Class;I)V";
  private static final String BEFORE_INTRINSIC_LOCK_ACQUISITION = "beforeIntrinsicLockAcquisition";
  private static final String BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE = "(Ljava/lang/Object;ZZLjava/lang/Class;I)V";
  private static final String FIELD_ACCESS = "fieldAccess";
  private static final String FIELD_ACCESS_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/reflect/Field;Ljava/lang/Class;I)V";
  private static final String METHOD_CALL = "methodCall";
  private static final String METHOD_CALL_SIGNATURE = "(ZLjava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;I)V";
  
  // Flashlight classes and methods
  private static final String FLASHLIGHT_RUNTIME_SUPPORT = "com/surelogic/_flashlight/rewriter/runtime/FlashlightRuntimeSupport";
  private static final String REPORT_FATAL_ERROR = "reportFatalError";
  private static final String REPORT_FATAL_ERROR_SIGNATURE = "(Ljava/lang/Exception;)V";
  
  private static final String FLASHLIGHT_EXCEPTION = "com/surelogic/_flashlight/rewriter/runtime/FlashlightRuntimeException";
  private static final String FLASHLIGHT_EXCEPTION_SIGNATURE = "(Ljava/lang/Exception;)V";
  
  // Other Java classes and methods
  private static final String CONSTRUCTOR = "<init>";
  
  private static final String JAVA_LANG_CLASS = "java/lang/Class";
  private static final String FOR_NAME = "forName";
  private static final String FOR_NAME_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";
  private static final String GET_DECLARED_FIELD = "getDeclaredField";
  private static final String GET_DECLARED_FIELD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/reflect/Field;";

  private static final String JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java/lang/ClassNotFoundException";
  
  private static final String JAVA_LANG_NO_SUCH_FIELD_EXCEPTION = "java/lang/NoSuchFieldException";


  
  /** The name of the sourcefile that contains the class being rewritten. */
  private final String sourceFileName;
  
  /** The internal name of the class being rewritten. */
  private final String classBeingAnalyzedInternal;
  /** The fully qualified name of the class being rewritten. */
  private final String classBeingAnalyzedFullyQualified;
  
  /** The simple name of the method being rewritten. */
  private final String methodName;
  /** Are we visiting the class initializer method? */
  private final boolean isClassInitializer;
  
  /**
   * The current source line of code being rewritten. Driven by calls to
   * {@link #visitLineNumber}. This is {@code -1} when no line number
   * information is available.
   */
  private int currentSrcLine = -1;
  
  /**
   * The amount by which the stack depth must be increased.
   */
  private int stackDepthDelta = 0;
  
  /**
   * The label for the exception handler, if necessary.  This starts out as 
   * {@code null}.  If the method needs one, as indicated by a call to
   * {@link #getFlashlightExceptionHandlerLabel()}, this field is initialized to a new
   * label (at most once).  When non-{@code null} at the start of
   * {@link #visitMaxs()}, an exceptional handler is inserted using 
   * {@link #insertFlashlightExceptionHandler()}.
   */
  private Label exceptionHandlerLabel = null;
  
  
  
  /**
   * Create a new method rewriter.
   * 
   * @param fname
   *          The name of the sourcefile that contains the class being
   *          rewritten.
   * @param nameInternal
   *          The internal name of the class being rewritten.
   * @param nameFullyQualified
   *          The fully qualified name of the class being rewritten.
   * @param mname
   *          The simple name of the method being rewritten.
   * @param isClassInit
   *          Is the visitor visiting the class initialization method
   *          "&lt;clinit&gt;"?
   * @param mv
   *          The {@code MethodVisitor} to delegate to.
   */
  public FlashlightMethodRewriter(final String fname,
      final String nameInternal, final String nameFullyQualified,
      final String mname, final boolean isClassInit, final MethodVisitor mv) {
    super(mv);
    sourceFileName = fname;
    classBeingAnalyzedInternal = nameInternal;
    classBeingAnalyzedFullyQualified = nameFullyQualified;
    methodName = mname;
    isClassInitializer = isClassInit;
  }
  
  
  
  @Override
  public void visitCode() {
    mv.visitCode();
    
    // Initialize the flashlight$inClass field
    if (isClassInitializer) {
      insertClassInitializerCode();
    }
  }
  
  @Override
  public void visitLineNumber(final int line, final Label start) {
    mv.visitLineNumber(line, start);
    currentSrcLine = line;
  }
  
  @Override
  public void visitInsn(final int opcode) {
    if (opcode == Opcodes.MONITORENTER && Properties.REWRITE_MONITORENTER) {
      rewriteMonitorenter();
    } else if (opcode == Opcodes.MONITOREXIT && Properties.REWRITE_MONITOREXIT) {
      rewriteMonitorexit();
    } else {
      mv.visitInsn(opcode);
    }
  }
  
  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    if (opcode == Opcodes.PUTFIELD && Properties.REWRITE_PUTFIELD) {
      rewritePutfield(owner, name, desc);
    } else if (opcode == Opcodes.PUTSTATIC && Properties.REWRITE_PUTSTATIC) {
      rewritePutstatic(owner, name, desc);
    } else if (opcode == Opcodes.GETFIELD && Properties.REWRITE_GETFIELD) {
      rewriteGetfield(owner, name, desc);
    } else if (opcode == Opcodes.GETSTATIC && Properties.REWRITE_GETSTATIC) {
      rewriteGetstatic(owner, name, desc);
    } else {
      mv.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc) {
    if (opcode == Opcodes.INVOKESTATIC) {
      rewriteInvokeStatic(owner, name, desc);
    } else {
      mv.visitMethodInsn(opcode, owner, name, desc);
    }
  }
  
  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    // Insert the exception handler if needed
    if (exceptionHandlerLabel != null) {
      insertFlashlightExceptionHandler();
    }

    mv.visitMaxs(maxStack + stackDepthDelta, maxLocals);
  }

  
  
  // =========================================================================
  // == Utility methods
  // =========================================================================

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
   * Generate code to push an integer constant.  Optimizes for whether the
   * integer fits in 8, 16, or 32 bits.
   * @param v The integer to push onto the stack.
   */
  private void pushIntegerConstant(final int v) {
    if (v < 128) {
      mv.visitIntInsn(Opcodes.BIPUSH, v);
    } else if (v < 32767) {
      mv.visitIntInsn(Opcodes.SIPUSH, v);
    } else {
      mv.visitLdcInsn(Integer.valueOf(v));
    }
  }
  
  /**
   * Generate code to push a Boolean constant.
   * @param b The Boolean value to push onto the stack.
   */
  private void pushBooleanConstant(final boolean b) {
    if (b) {
      mv.visitInsn(Opcodes.ICONST_1);
    } else {
      mv.visitInsn(Opcodes.ICONST_0);
    }
  }

  
  
  // =========================================================================
  // == Insert Fatal error exception handler
  // =========================================================================

  private Label getFlashlightExceptionHandlerLabel() {
    if (exceptionHandlerLabel == null) {
      exceptionHandlerLabel = new Label();
    }
    return exceptionHandlerLabel;
  }
  
  private void insertFlashlightExceptionHandler() {
    mv.visitLabel(exceptionHandlerLabel);
    
    // Exception
    mv.visitInsn(Opcodes.DUP);
    // Exception, Exception
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_RUNTIME_SUPPORT, REPORT_FATAL_ERROR, REPORT_FATAL_ERROR_SIGNATURE);
    // Exception    
    mv.visitTypeInsn(Opcodes.NEW, FLASHLIGHT_EXCEPTION);
    // Exception, FlashlightException
    mv.visitInsn(Opcodes.DUP_X1);
    // FlashlightException, Exception, FlashlightException
    mv.visitInsn(Opcodes.SWAP);
    // FlashlightException, FlashlightException, Exception
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, FLASHLIGHT_EXCEPTION, CONSTRUCTOR, FLASHLIGHT_EXCEPTION_SIGNATURE);
    // FlashlightException
    mv.visitInsn(Opcodes.ATHROW);
    
    updateStackDepthDelta(3);
  }
  
  

  // =========================================================================
  // == Insert Bookkeeping code
  // =========================================================================

  private void insertClassInitializerCode() {
    // Stack is empty (we are at the beginning of the method!)
    
    /* We need to insert the expression "Class.forName(<fully-qualified-class-name>)"
     * into the code.  We have to introduce a try-catch for the call.
     */
    final Label tryStart = new Label();
    final Label tryEnd = new Label();
    final Label catchClassNotFound = getFlashlightExceptionHandlerLabel();
    mv.visitTryCatchBlock(tryStart, tryEnd, catchClassNotFound, JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
    mv.visitLdcInsn(classBeingAnalyzedFullyQualified);
    // className
    mv.visitLabel(tryStart);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, JAVA_LANG_CLASS, FOR_NAME, FOR_NAME_SIGNATURE);
    // Class
    mv.visitLabel(tryEnd);
    mv.visitFieldInsn(Opcodes.PUTSTATIC, classBeingAnalyzedInternal, FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // empty stack

    // resume
    
    updateStackDepthDelta(1);
  }

  
  
  // =========================================================================
  // == Rewrite field accesses
  // =========================================================================

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
    pushBooleanConstant(false);
    // Stack is "..., objectref, false"
    mv.visitInsn(Opcodes.SWAP);
    // Stack is "..., false, objectref"
    
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
    pushBooleanConstant(true);
    // Stack is "..., value, objectref, true"
    mv.visitInsn(Opcodes.SWAP);
    // Stack is "..., value, true, objectref"
    
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
    pushBooleanConstant(false);
    // Stack is "..., false"
    mv.visitInsn(Opcodes.ACONST_NULL);
    // Stack is "..., false, null"
    
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
    pushBooleanConstant(true);
    // Stack is "..., value, true"
    mv.visitInsn(Opcodes.ACONST_NULL);
    // Stack is "..., value, true, null"
    
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
    final Label try1End = new Label();
    final Label try2Start = new Label();
    final Label try2End = new Label();
    final Label catchClassNotFound = getFlashlightExceptionHandlerLabel();
    final Label catchNoSuchField = getFlashlightExceptionHandlerLabel();
    mv.visitTryCatchBlock(try1Start, try1End, catchClassNotFound, JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
    mv.visitTryCatchBlock(try2Start, try2End, catchNoSuchField, JAVA_LANG_NO_SUCH_FIELD_EXCEPTION);
    
    /* We need to insert the expression
     * "Class.forName(<owner>).getDeclaredField(<name>)" into the code.  This puts
     * the java.lang.reflect.Field object for the accessed field on the stack.
     */
    mv.visitLdcInsn(fullyQualifiedOwner);
    mv.visitLabel(try1Start);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, JAVA_LANG_CLASS, FOR_NAME, FOR_NAME_SIGNATURE);
    mv.visitLabel(try1End);
    mv.visitLdcInsn(name);
    mv.visitLabel(try2Start);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JAVA_LANG_CLASS, GET_DECLARED_FIELD, GET_DECLARED_FIELD_SIGNATURE);
    mv.visitLabel(try2End);
    // Stack is "..., isRead, receiver, Field"
    
    /* We need to insert the expression "Class.forName(<current_class>)"
     * to push the java.lang.Class object of the referencing class onto the 
     * stack.
     */
    mv.visitFieldInsn(Opcodes.GETSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // Stack is "..., isRead, receiver, Field, inClass"
    
    /* Push the line number of the field access. */
    pushIntegerConstant(currentSrcLine);
    // Stack is "..., isRead, receiver, Field, inClass, LineNumber"
    
    /* We can now call Store.fieldAccess() */
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE, FIELD_ACCESS, FIELD_ACCESS_SIGNATURE);    
    // Stack is "..."
    
    // Resume
  }

  
  
  // =========================================================================
  // == Rewrite monitor methods
  // =========================================================================

  private void rewriteMonitorenter() {
    // ..., obj  
    
    /* We begin by partially setting up the stack for the post-synchronized
     * call.  We set up the first two arguments to the method.
     */
    
    /* Copy the object being locked for use as the first parameter */
    mv.visitInsn(Opcodes.DUP);
    // ..., obj, obj  
    /* Copy again to use in the pre-synchronized call */
    mv.visitInsn(Opcodes.DUP);
    // ..., obj, obj, obj  
    
    /* Get the Class object for the class being analyzed. We need to both
     * compare this object with the object being locked and use it as a
     * parameter for the pre- and post-synchronized call.
     */
    mv.visitFieldInsn(Opcodes.GETSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // ..., obj, obj, obj, inClass

    /* Copy the class object three values down to use as the second parameter
     * in the post-synchronized call.
     */
    mv.visitInsn(Opcodes.DUP_X2);
    // ..., obj, inClass, obj,   obj, inClass  

    /* Make some more copies of the object being locked for comparison purposes */
    mv.visitInsn(Opcodes.SWAP);
    // ..., obj, inClass, obj,   inClass, obj  
    mv.visitInsn(Opcodes.DUP_X1);
    // ..., obj, inClass, obj,   obj, inClass, obj  
    mv.visitInsn(Opcodes.DUP);
    // ..., obj, inClass, obj,   obj, inClass, obj, obj

    /* Compare the object against "this" */
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    // ..., obj, inClass, obj,   obj, inClass, obj, obj, this
    final Label pushFalse1 = new Label();
    final Label afterPushIsThis = new Label();
    mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse1);
    pushBooleanConstant(true);
    mv.visitJumpInsn(Opcodes.GOTO, afterPushIsThis);
    mv.visitLabel(pushFalse1);
    pushBooleanConstant(false);
    mv.visitLabel(afterPushIsThis);
    // ..., obj, inClass, obj,   obj, inClass, obj, isThis
    
    /* Copy the comparison result three values down to use as the
     * second parameter to the pre-synchronized call, and then
     * dispose of the original
     */
    mv.visitInsn(Opcodes.DUP_X2);
    // ..., obj, inClass, obj,   obj, isThis, inClass, obj, isThis
    mv.visitInsn(Opcodes.POP);
    // ..., obj, inClass, obj,   obj, isThis, inClass, obj
    
    /* Rotate up the stack the Class object, and then put a copy
     * two values down to use as the 4th parameter to the pre-call.
     * (We rotate it into the correct stack position later.)
     */
    mv.visitInsn(Opcodes.SWAP);
    // ..., obj, inClass, obj,   obj, isThis, obj, inClass
    mv.visitInsn(Opcodes.DUP_X1);
    // ..., obj, inClass, obj,   obj, isThis, inClass, obj, inClass

    /* Compare the object being locked against the Class object */
    final Label pushFalse2 = new Label();
    final Label afterPushIsClass = new Label();
    mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse2);
    pushBooleanConstant(true);
    mv.visitJumpInsn(Opcodes.GOTO, afterPushIsClass);
    mv.visitLabel(pushFalse2);
    pushBooleanConstant(false);
    mv.visitLabel(afterPushIsClass);
    // ..., obj, inClass, obj,   obj, isThis, inClass, isClass
    
    /* Swap the orger of inClass and isClass to correctly establish
     * the 3rd and 4th parameters to the pre-call
     */
    mv.visitInsn(Opcodes.SWAP);
    // ..., obj, inClass, obj,   obj, isThis, isClass, inClass
    
    /* Push the lineNumber and call the pre-sychronized method */
    pushIntegerConstant(currentSrcLine);
    // ..., obj, inClass, obj,   obj, isThis, isClass, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE,
        BEFORE_INTRINSIC_LOCK_ACQUISITION,
        BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // ..., obj, inClass, obj
    
    /* The original monitor enter call */
    mv.visitInsn(Opcodes.MONITORENTER);
    // ..., obj, inClass
    
    /* Push the 3rd parameter for the post-synchronized call and call it */
    pushIntegerConstant(currentSrcLine);
    // ..., obj, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE,
        AFTER_INTRINSIC_LOCK_ACQUISITION,
        AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // ...
    
    /* Resume original instruction stream */

    updateStackDepthDelta(7);
  }

  private void rewriteMonitorexit() {
    // ..., obj  
    
    /* Copy the object being locked for use as the first parameter to
     * Store.afterInstrinsicLockRelease().
     */
    mv.visitInsn(Opcodes.DUP);
    // ..., obj, obj  

    /* The original monitor exit call */
    mv.visitInsn(Opcodes.MONITOREXIT);
    // ..., obj
    
    /* Get the Class object for the class being analyzed. */
    mv.visitFieldInsn(Opcodes.GETSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // ..., obj, inClass
    
    /* Push the lineNumber and call the Store method. */
    pushIntegerConstant(currentSrcLine);
    // ..., obj, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE,
        AFTER_INTRINSIC_LOCK_RELEASE, AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE);
    // ...

    /* Resume original instruction stream */

    updateStackDepthDelta(2);
  }

  
  
  // =========================================================================
  // == Rewrite method calls
  // =========================================================================

  private void rewriteInvokeStatic(
      final String owner, final String name, final String desc) {
    // arg_1, ..., arg_n
    pushBooleanConstant(true);
    // arg_1, ..., arg_n, true
    mv.visitInsn(Opcodes.ACONST_NULL);
    // arg_1, ..., arg_n, true, null
    mv.visitLdcInsn(sourceFileName);
    // arg_1, ..., arg_n, true, null, filename
    mv.visitLdcInsn(methodName);
    // arg_1, ..., arg_n, true, null, filename, mname
    mv.visitFieldInsn(Opcodes.GETSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // arg_1, ..., arg_n, true, null, filename, mname, inClass
    pushIntegerConstant(currentSrcLine);
    // arg_1, ..., arg_n, true, null, filename, mname, inClass, line
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE, METHOD_CALL, METHOD_CALL_SIGNATURE);
    // arg_1, ..., arg_n
    
    // The original method call
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc);
    
    // arg_1, ..., arg_n
    pushBooleanConstant(false);
    // arg_1, ..., arg_n, false
    mv.visitInsn(Opcodes.ACONST_NULL);
    // arg_1, ..., arg_n, false, null
    mv.visitLdcInsn(sourceFileName);
    // arg_1, ..., arg_n, false, null, filename
    mv.visitLdcInsn(methodName);
    // arg_1, ..., arg_n, false, null, filename, mname
    mv.visitFieldInsn(Opcodes.GETSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // arg_1, ..., arg_n, false, null, filename, mname, inClass
    pushIntegerConstant(currentSrcLine);
    // arg_1, ..., arg_n, false, null, filename, mname, inClass, line
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FLASHLIGHT_STORE, METHOD_CALL, METHOD_CALL_SIGNATURE);

    // Resume code
    
    updateStackDepthDelta(6);
  }
}
