package com.surelogic._flashlight.rewriter;

import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class FlashlightMethodRewriter extends MethodAdapter {
  private static final String ENCLOSING_THIS_PREFIX = "this$";
  
  private static final String CLASS_INITIALIZER = "<clinit>";
  private static final String INITIALIZER = "<init>";
  
  

  /** The name of the source file that contains the class being rewritten. */
  private final String sourceFileName;
  
  /** The internal name of the class being rewritten. */
  private final String classBeingAnalyzedInternal;
  /** The fully qualified name of the class being rewritten. */
  private final String classBeingAnalyzedFullyQualified;
  
  /** The simple name of the method being rewritten. */
  private final String methodName;
  /** Are we visiting a constructor? */
  private final boolean isConstructor;
  /** Are we visiting the class initializer method? */
  private final boolean isClassInitializer;
  /** Was the method originally synchronized? */
  private final boolean wasSynchronized;
  /** Is the method static? */
  private final boolean isStatic;
  
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
   * The global list of wrapper methods that need to be created.  This list
   * is added to by this class, and is provided by the FlashlightClassRewriter
   * instance that create the method rewriter.
   */
  private final Set<MethodCallWrapper> wrapperMethods;
  
  /**
   * Label for the start of the original method code, used for rewriting
   * synchronized methods 
   */
  private Label startOfOriginalMethod = null;
  /**
   * Label marking the end of the original method code (including the 
   * inserted flashlight exception handler), used for rewriting synchronized
   * methods.
   */
  private Label endOfOriginalBody_startOfExceptionHandler = null;
  
  
  
  /**
   * Create a new method rewriter.
   * 
   * @param fname
   *          The name of the source file that contains the class being
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
   * @param wrappers
   *          The set of wrapper methods that this visitor should add to.
   * @param _synchronized
   *          Was the method originally synchronized?
   * @param _static 
   *          Is the method static?
   * @param mv
   *          The {@code MethodVisitor} to delegate to.
   */
  public FlashlightMethodRewriter(final String fname,
      final String nameInternal, final String nameFullyQualified,
      final String mname, final Set<MethodCallWrapper> wrappers, final int access,
      final MethodVisitor mv) {
    super(mv);
    sourceFileName = fname;
    classBeingAnalyzedInternal = nameInternal;
    classBeingAnalyzedFullyQualified = nameFullyQualified;
    methodName = mname;
    isConstructor = mname.equals(INITIALIZER);
    isClassInitializer = mname.equals(CLASS_INITIALIZER);
    wrapperMethods = wrappers;
    wasSynchronized = (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    isStatic = (access & Opcodes.ACC_STATIC) != 0;
  }
  
  
  
  @Override
  public void visitCode() {
    mv.visitCode();
    
    // Initialize the flashlight$inClass field
    if (isClassInitializer) {
      insertClassInitializerCode();
    } else if (wasSynchronized && Properties.REWRITE_SYNCHRONIZED_METHOD) {
      insertSynchronizedMethodPrefix();
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
    } else if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
      if (wasSynchronized && Properties.REWRITE_SYNCHRONIZED_METHOD) {
        insertSynchronizedMethodExit();
      }
      mv.visitInsn(opcode);
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
    if (opcode == Opcodes.INVOKEVIRTUAL && Properties.REWRITE_INVOKEVIRTUAL) {
      rewriteMethodCall(opcode, owner, name, desc);
    } else if (opcode == Opcodes.INVOKESPECIAL && Properties.REWRITE_INVOKESPECIAL) {
      if (!name.equals(FlashlightNames.CONSTRUCTOR)) {
        rewriteMethodCall(Opcodes.INVOKESPECIAL, owner, name, desc);
      } else {
        mv.visitMethodInsn(opcode, owner, name, desc);
      }
    } else if (opcode == Opcodes.INVOKEINTERFACE && Properties.REWRITE_INVOKEINTERFACE) {
      rewriteMethodCall(Opcodes.INVOKEINTERFACE, owner, name, desc);
    } else if (opcode == Opcodes.INVOKESTATIC && Properties.REWRITE_INVOKESTATIC) {
      rewriteMethodCall(Opcodes.INVOKESTATIC, owner, name, desc);
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
    
    if (wasSynchronized && Properties.REWRITE_SYNCHRONIZED_METHOD) {
      insertSynchronizedMethodPostfix();
    }
    
    super.visitMaxs(maxStack + stackDepthDelta, maxLocals);
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
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
        FlashlightNames.FLASHLIGHT_RUNTIME_SUPPORT,
        FlashlightNames.REPORT_FATAL_ERROR, FlashlightNames.REPORT_FATAL_ERROR_SIGNATURE);
    // Exception    
    mv.visitTypeInsn(Opcodes.NEW, FlashlightNames.FLASHLIGHT_EXCEPTION);
    // Exception, FlashlightException
    mv.visitInsn(Opcodes.DUP_X1);
    // FlashlightException, Exception, FlashlightException
    mv.visitInsn(Opcodes.SWAP);
    // FlashlightException, FlashlightException, Exception
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, FlashlightNames.FLASHLIGHT_EXCEPTION, FlashlightNames.CONSTRUCTOR, FlashlightNames.FLASHLIGHT_EXCEPTION_SIGNATURE);
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
    mv.visitTryCatchBlock(tryStart, tryEnd, catchClassNotFound, FlashlightNames.JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
    mv.visitLdcInsn(classBeingAnalyzedFullyQualified);
    // className
    mv.visitLabel(tryStart);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.JAVA_LANG_CLASS, FlashlightNames.FOR_NAME, FlashlightNames.FOR_NAME_SIGNATURE);
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
   * Is the field a compiler-generated <code>this$</code> field used by nested
   * classes?  We should really be checking whether the field is synthetic,
   * but that would require linking.
   */
  private static boolean isEnclosingThisField(final String name) {
    return name.startsWith(ENCLOSING_THIS_PREFIX);
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
    if (isEnclosingThisField(name)) {
      /* Don't instrument enclosing this references */
      mv.visitFieldInsn(Opcodes.PUTFIELD, owner, name, desc);
      return;
    }
        
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
    ByteCodeUtils.pushBooleanConstant(mv, false);
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
    if (isEnclosingThisField(name)) {
      /* Don't instrument enclosing this references */
      mv.visitFieldInsn(Opcodes.PUTFIELD, owner, name, desc);
      return;
    }

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
    ByteCodeUtils.pushBooleanConstant(mv, true);
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
    ByteCodeUtils.pushBooleanConstant(mv, false);
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
    ByteCodeUtils.pushBooleanConstant(mv, true);
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
     * basis here in the bytecode, with the two exceptions sharing the same
     * handler.  Furthermore, there is only one handler for the entire method.
     * This is not expressible in the Java source code.
     */
    final Label try1Start = new Label();
    final Label try1End = new Label();
    final Label try2Start = new Label();
    final Label try2End = new Label();
    final Label catchClassNotFound = getFlashlightExceptionHandlerLabel();
    final Label catchNoSuchField = getFlashlightExceptionHandlerLabel();
    mv.visitTryCatchBlock(try1Start, try1End, catchClassNotFound, FlashlightNames.JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
    mv.visitTryCatchBlock(try2Start, try2End, catchNoSuchField, FlashlightNames.JAVA_LANG_NO_SUCH_FIELD_EXCEPTION);
    
    /* We need to insert the expression
     * "Class.forName(<owner>).getDeclaredField(<name>)" into the code.  This puts
     * the java.lang.reflect.Field object for the accessed field on the stack.
     */
    mv.visitLdcInsn(fullyQualifiedOwner);
    mv.visitLabel(try1Start);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.JAVA_LANG_CLASS, FlashlightNames.FOR_NAME, FlashlightNames.FOR_NAME_SIGNATURE);
    mv.visitLabel(try1End);
    mv.visitLdcInsn(name);
    mv.visitLabel(try2Start);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, FlashlightNames.JAVA_LANG_CLASS, FlashlightNames.GET_DECLARED_FIELD, FlashlightNames.GET_DECLARED_FIELD_SIGNATURE);
    mv.visitLabel(try2End);
    // Stack is "..., isRead, receiver, Field"
    
    /* We need to insert the expression "Class.forName(<current_class>)"
     * to push the java.lang.Class object of the referencing class onto the 
     * stack.
     */
    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
    // Stack is "..., isRead, receiver, Field, inClass"
    
    /* Push the line number of the field access. */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // Stack is "..., isRead, receiver, Field, inClass, LineNumber"
    
    /* We can now call Store.fieldAccess() */
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE, FlashlightNames.FIELD_ACCESS, FlashlightNames.FIELD_ACCESS_SIGNATURE);    
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
    
    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);

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
    ByteCodeUtils.pushBooleanConstant(mv, true);
    mv.visitJumpInsn(Opcodes.GOTO, afterPushIsThis);
    mv.visitLabel(pushFalse1);
    ByteCodeUtils.pushBooleanConstant(mv, false);
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
    ByteCodeUtils.pushBooleanConstant(mv, true);
    mv.visitJumpInsn(Opcodes.GOTO, afterPushIsClass);
    mv.visitLabel(pushFalse2);
    ByteCodeUtils.pushBooleanConstant(mv, false);
    mv.visitLabel(afterPushIsClass);
    // ..., obj, inClass, obj,   obj, isThis, inClass, isClass
    
    /* Swap the orger of inClass and isClass to correctly establish
     * the 3rd and 4th parameters to the pre-call
     */
    mv.visitInsn(Opcodes.SWAP);
    // ..., obj, inClass, obj,   obj, isThis, isClass, inClass
    
    /* Push the lineNumber and call the pre-sychronized method */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., obj, inClass, obj,   obj, isThis, isClass, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // ..., obj, inClass, obj
    
    /* The original monitor enter call */
    mv.visitInsn(Opcodes.MONITORENTER);
    // ..., obj, inClass
    
    /* Push the 3rd parameter for the post-synchronized call and call it */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., obj, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
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
    
    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
    
    /* Push the lineNumber and call the Store method. */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., obj, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
        FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE, FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE);
    // ...

    /* Resume original instruction stream */

    updateStackDepthDelta(2);
  }

  
  
  // =========================================================================
  // == Rewrite synchronized methods
  // =========================================================================
  
  private void pushSynchronizedMethodLockObject() {
    if (isStatic) {
      ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
    } else {
      mv.visitVarInsn(Opcodes.ALOAD, 0);
    }
  }
  
  private void insertSynchronizedMethodPrefix() {
    // empty stack
    
    /* First call Store.beforeInstrinsicLockAcquisition() */
    pushSynchronizedMethodLockObject();
    // lockObj
    ByteCodeUtils.pushBooleanConstant(mv, !isStatic);
    // lockObj, isReceiver
    ByteCodeUtils.pushBooleanConstant(mv, isStatic);
    // lockObj, isReceiver, isStatic
    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
    // lockObj, isReceiver, isStatic, inClass
    ByteCodeUtils.pushIntegerConstant(mv, 0);
    // lockObj, isReceiver, isStatic, inClass, 0
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // empty stack
    
    /* Insert the explicit monitor acquisition */
    pushSynchronizedMethodLockObject();
    // lockObj
    mv.visitInsn(Opcodes.MONITORENTER);
    // empty stack
    
    /* Now call Store.afterIntrinsicLockAcquisition */
    pushSynchronizedMethodLockObject();
    // lockObj
    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
    // lockObj, inClass
    ByteCodeUtils.pushIntegerConstant(mv, 0);
    // lockObj, inClass, 0
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // empty stack
    
    startOfOriginalMethod = new Label();
    endOfOriginalBody_startOfExceptionHandler = new Label();
    mv.visitTryCatchBlock(startOfOriginalMethod,
        endOfOriginalBody_startOfExceptionHandler,
        endOfOriginalBody_startOfExceptionHandler, null);
    mv.visitLabel(startOfOriginalMethod);
    // Resume code
    
    updateStackDepthDelta(5);
  }
  
  private void insertSynchronizedMethodPostfix() {
    mv.visitLabel(endOfOriginalBody_startOfExceptionHandler);
    
    // exception 
    insertSynchronizedMethodExit();
    // exception
    
    /* Rethrow the exception */
    mv.visitInsn(Opcodes.ATHROW);
    
    /* Should update the stack depth, but we know we only get executed if
     * insertSynchronizedMethodPrefix() is run, and that already updates the
     * stack depth by 5, which is more than we need here (4).
     */ 
  }

  private void insertSynchronizedMethodExit() {
    // ...
    
    /* Explicitly release the lock */
    pushSynchronizedMethodLockObject();
    // ..., lockObj
    mv.visitInsn(Opcodes.MONITOREXIT);
    // ...
    
    /* Call Store.afterIntrinsicLockRelease(). */
    pushSynchronizedMethodLockObject();
    // ..., lockObj
    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
    // ..., lockObj, inClass
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., lockObj, inClass, exitLineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
        FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE, FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE);
    // ...
    
    // Resume code

    /* Should update the stack depth, but we know we only get executed if
     * insertSynchronizedMethodPrefix() is run, and that already updates the
     * stack depth by 5, which is more than we need here (3).
     */ 
  }

  
  
  // =========================================================================
  // == Rewrite method calls
  // =========================================================================
  
  private void rewriteMethodCall(final int opcode,
      final String owner, final String name, final String desc) {
    /* Create the wrapper method information and add it to the list of wrappers */
    final MethodCallWrapper wrapper;
    if (opcode == Opcodes.INVOKESPECIAL) {
      wrapper = new SpecialCallWrapper(owner, name, desc, opcode);
    } else if (opcode == Opcodes.INVOKESTATIC){
      wrapper = new StaticCallWrapper(owner, name, desc, opcode);
    } else {
      wrapper = new InterfaceAndVirtualCallWrapper(owner, name, desc, opcode);
    }
    wrapperMethods.add(wrapper);
    
    // ..., [objRef], arg1, ..., argN
    mv.visitLdcInsn(methodName);
    // ..., [objRef], arg1, ..., argN, callingMethodName    
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., [objRef], arg1, ..., argN, callingMethodName, sourceLine
    wrapper.invokeWrapperMethod(mv, classBeingAnalyzedInternal);
    // ..., [returnVlaue]
    
    updateStackDepthDelta(2);
  }



//  private void insertConstructorExecution(final boolean before) {
//    // ...
//    ByteCodeUtils.pushBooleanConstant(mv, before);
//    // ..., before
//    mv.visitVarInsn(Opcodes.ALOAD, 0);
//    // ..., before, this
//    ByteCodeUtils.pushInClass(mv, classBeingAnalyzedInternal);
//    // ..., before, this, inClass
//    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
//    // ..., before, this, inClass, exitLineNumber
//    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
//        "constructorExecution", "(ZLjava/lang/Object;Ljava/lang/Class;I)V");
//    // ...
//    
//    updateStackDepthDelta(4);
//  }
}
