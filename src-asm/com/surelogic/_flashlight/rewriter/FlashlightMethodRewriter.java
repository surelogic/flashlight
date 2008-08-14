package com.surelogic._flashlight.rewriter;

import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.surelogic._flashlight.rewriter.ConstructorInitStateMachine.Callback;

final class FlashlightMethodRewriter extends MethodAdapter {
  private static final String CLASS_INITIALIZER = "<clinit>";
  private static final String INITIALIZER = "<init>";
  
  
  
  /**
   * Call back for the constructor initialization state machine.
   * Clears the machine, and 
   * inserts the constructor execution begin event into the constructor code.
   */
  private final class ObjectInitCallback implements Callback {
    public void superConstructorCalled() {
      stateMachine = null;
      if (config.rewriteConstructorExecution) {
        insertConstructorExecutionPrefix();
      }
    }
  }
  
  
  
  /** Configuration information, derived from properties. */
  private final Configuration config;

  /** Is the current classfile at least from Java 5? */
  private final boolean atLeastJava5;

  /** Is the current classfile an interface? */
  private final boolean inInterface;
  
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
   * The global list of wrapper methods that need to be created.  This list
   * is added to by this class, and is provided by the FlashlightClassRewriter
   * instance that create the method rewriter.
   */
  private final Set<MethodCallWrapper> wrapperMethods;
  
  /**
   * Label for the start of the original method code, used for rewriting
   * constructors and synchronized methods.
   */
  private Label startOfOriginalMethod = null;
  
  /**
   * Label marking the end of the original method code (including the 
   * inserted flashlight exception handler), used for rewriting constructors and synchronized
   * methods.
   */
  private Label endOfOriginalBody_startOfExceptionHandler = null;
  
  /**
   * If {@link #isConstructor} is <code>true</code>, this is initialized to
   * a state machine that fires after the super constructor call has been
   * detected; see {@link ObjectInitCallback}.  This field is nulled once
   * the call has been detected.
   */
  private ConstructorInitStateMachine stateMachine = null;
  
  /**
   * Local variable sorter used to manage the indices of local variables
   * so that we can pop arguments off the stack when processing method
   * calls inside if methods.
   */
  private LocalVariablesSorter lvs;
  
  
  
  public static MethodVisitor create(
      final int access, final String mname, final String desc,
      final MethodVisitor mv, final Configuration conf, final boolean java5,
      final boolean inInt, final String fname, final String nameInternal,
      final String nameFullyQualified, final Set<MethodCallWrapper> wrappers) {
    final FlashlightMethodRewriter methodRewriter =
      new FlashlightMethodRewriter(access, mname, desc, mv,
          conf, java5, inInt, fname, nameInternal, nameFullyQualified, wrappers);
    methodRewriter.lvs = new LocalVariablesSorter(access, desc, methodRewriter);
    return methodRewriter.lvs;
  }
  
  
  
  /**
   * Create a new method rewriter.
   * @param mname
   *          The simple name of the method being rewritten.
   * @param mv
   *          The {@code MethodVisitor} to delegate to.
   * @param fname
   *          The name of the source file that contains the class being
   *          rewritten.
   * @param nameInternal
   *          The internal name of the class being rewritten.
   * @param nameFullyQualified
   *          The fully qualified name of the class being rewritten.
   * @param wrappers
   *          The set of wrapper methods that this visitor should add to.
   */
  private FlashlightMethodRewriter(
      final int access, final String mname, final String desc,
      final MethodVisitor mv, final Configuration conf, final boolean java5,
      final boolean inInt, final String fname, final String nameInternal,
      final String nameFullyQualified, final Set<MethodCallWrapper> wrappers) {
    super(mv);
    config = conf;
    atLeastJava5 = java5;
    inInterface = inInt;
    wasSynchronized = (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    isStatic = (access & Opcodes.ACC_STATIC) != 0;
    methodName = mname;
    isConstructor = mname.equals(INITIALIZER);
    isClassInitializer = mname.equals(CLASS_INITIALIZER);
    sourceFileName = fname;
    classBeingAnalyzedInternal = nameInternal;
    classBeingAnalyzedFullyQualified = nameFullyQualified;
    wrapperMethods = wrappers;
    
    if (isConstructor) {
      stateMachine = new ConstructorInitStateMachine(new ObjectInitCallback());
    } else {
      stateMachine = null;
    }
  }
  
  
  
  @Override
  public void visitCode() {
    mv.visitCode();
    
    // Initialize the flashlight$inClass field
    if (!atLeastJava5 && isClassInitializer) {
      insertClassInitializerCode();
    } else if (wasSynchronized && config.rewriteSynchronizedMethod) {
      insertSynchronizedMethodPrefix();
    }
  }
  
  @Override
  public void visitLineNumber(final int line, final Label start) {
    mv.visitLineNumber(line, start);
    currentSrcLine = line;
  }
  
  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    mv.visitTypeInsn(opcode, type);
    if (stateMachine != null) stateMachine.visitTypeInsn(opcode, type);
  }
  
  @Override
  public void visitInsn(final int opcode) {
    if (opcode == Opcodes.MONITORENTER && config.rewriteMonitorenter) {
      rewriteMonitorenter();
    } else if (opcode == Opcodes.MONITOREXIT && config.rewriteMonitorexit) {
      rewriteMonitorexit();
    } else if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
      if (wasSynchronized && config.rewriteSynchronizedMethod) {
        insertSynchronizedMethodExit();
      }
      if (isConstructor && config.rewriteConstructorExecution) {
        insertConstructorExecution(false);
        // Max Stack height is already updated because insertConstructorExecutionPrefix() must have been run 
      }
      mv.visitInsn(opcode);
    } else {
      mv.visitInsn(opcode);
    }

    if (stateMachine != null) stateMachine.visitInsn(opcode);
  }
  
  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    if (opcode == Opcodes.PUTFIELD && config.rewritePutfield) {
      rewritePutfield(owner, name, desc);
    } else if (opcode == Opcodes.PUTSTATIC && config.rewritePutstatic) {
      rewritePutstatic(owner, name, desc);
    } else if (opcode == Opcodes.GETFIELD && config.rewriteGetfield) {
      rewriteGetfield(owner, name, desc);
    } else if (opcode == Opcodes.GETSTATIC && config.rewriteGetstatic) {
      rewriteGetstatic(owner, name, desc);
    } else {
      mv.visitFieldInsn(opcode, owner, name, desc);
    }
    
    if (stateMachine != null) stateMachine.visitFieldInsn(opcode, owner, name, desc);    
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc) {
    if (opcode == Opcodes.INVOKEVIRTUAL && config.rewriteInvokevirtual) {
      rewriteMethodCall(Opcodes.INVOKEVIRTUAL, owner, name, desc);
    } else if (opcode == Opcodes.INVOKESPECIAL && config.rewriteInvokespecial) {
      if (!name.equals(FlashlightNames.CONSTRUCTOR)) {
        rewriteMethodCall(Opcodes.INVOKESPECIAL, owner, name, desc);
      } else {
        if (config.rewriteInit) {
          rewriteConstructorCall(owner, name, desc);
        } else {
          mv.visitMethodInsn(opcode, owner, name, desc);
        }
      }
    } else if (opcode == Opcodes.INVOKEINTERFACE && config.rewriteInvokeinterface) {
      rewriteMethodCall(Opcodes.INVOKEINTERFACE, owner, name, desc);
    } else if (opcode == Opcodes.INVOKESTATIC && config.rewriteInvokestatic) {
      rewriteMethodCall(Opcodes.INVOKESTATIC, owner, name, desc);
    } else { // Unknown, but safe
      mv.visitMethodInsn(opcode, owner, name, desc);
    }

    if (stateMachine != null) stateMachine.visitMethodInsn(opcode, owner, name, desc);
  }
  
  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    if (wasSynchronized && config.rewriteSynchronizedMethod) {
      insertSynchronizedMethodPostfix();
    }
    
    if (isConstructor && config.rewriteConstructorExecution) {
      insertConstructorExecutionPostfix();
    }
    
    mv.visitMaxs(maxStack + stackDepthDelta, maxLocals);
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    if (stateMachine != null) stateMachine.visitIntInsn(opcode, operand);
    mv.visitIntInsn(opcode, operand);
  }
  
  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    if (stateMachine != null) stateMachine.visitJumpInsn(opcode, label);
    mv.visitJumpInsn(opcode, label);
  }
  
  @Override
  public void visitLabel(final Label label) {
    if (stateMachine != null) stateMachine.visitLabel(label);
    mv.visitLabel(label);
  }
  
  @Override
  public void visitLdcInsn(final Object cst) {
    if (stateMachine != null) stateMachine.visitLdcInsn(cst);
    mv.visitLdcInsn(cst);
  }
  
  @Override
  public void visitLookupSwitchInsn(
      final Label dflt, final int[] keys, final Label[] labels) {
    if (stateMachine != null) stateMachine.visitLookupSwitchInsn(dflt, keys, labels);
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }
  
  @Override
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    if (stateMachine != null) stateMachine.visitMultiANewArrayInsn(desc, dims);
    mv.visitMultiANewArrayInsn(desc, dims);
  }
  
  @Override
  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label[] labels) {
    if (stateMachine != null) stateMachine.visitTableSwitchInsn(min, max, dflt, labels);
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }
  
  @Override
  public void visitVarInsn(final int opcode, final int var) {
    if (stateMachine != null) stateMachine.visitVarInsn(opcode, var);
    mv.visitVarInsn(opcode, var);
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
  // == Insert Bookkeeping code
  // =========================================================================

  private void insertClassInitializerCode() {
    // Stack is empty (we are at the beginning of the method!)
    
    /* We need to insert the expression "Class.forName(<fully-qualified-class-name>)"
     * into the code.
     */
    mv.visitLdcInsn(classBeingAnalyzedFullyQualified);
    // className
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_RUNTIME_SUPPORT, FlashlightNames.GET_CLASS, FlashlightNames.GET_CLASS_SIGNATURE);
    // Class
    mv.visitFieldInsn(Opcodes.PUTSTATIC, classBeingAnalyzedInternal, FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC);
    // empty stack

    // resume
    
    updateStackDepthDelta(1);
  }

  
  
  // =========================================================================
  // == Rewrite new/<init>
  // =========================================================================
  
  private void insertConstructorExecutionPrefix() {
    /* Create event */
    insertConstructorExecution(true);
    
    /* Set up finally handler */
    startOfOriginalMethod = new Label();
    endOfOriginalBody_startOfExceptionHandler = new Label();
    mv.visitTryCatchBlock(startOfOriginalMethod,
        endOfOriginalBody_startOfExceptionHandler,
        endOfOriginalBody_startOfExceptionHandler, null);
    
    /* Start of constructor */
    mv.visitLabel(startOfOriginalMethod);
    
    updateStackDepthDelta(4);
  }
  
  private void insertConstructorExecutionPostfix() {
    mv.visitLabel(endOfOriginalBody_startOfExceptionHandler);
    
    // exception 
    insertConstructorExecution(false); // +4 on stack
    // exception
    
    /* Rethrow the exception */
    mv.visitInsn(Opcodes.ATHROW);
    
    updateStackDepthDelta(5);
  }
  
  private void insertConstructorExecution(final boolean before) {
    // ...
    ByteCodeUtils.pushBooleanConstant(mv, before);
    // ..., before
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    // ..., before, this
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // ..., before, this, inClass
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., before, this, inClass, line
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName, FlashlightNames.CONSTRUCTOR_EXECUTION, FlashlightNames.CONSTRUCTOR_EXECUTION_SIGNATURE);
    // ...
  }
  
  private void rewriteConstructorCall(
      final String owner, final String name, final String desc) {
    if (isConstructor && stateMachine != null) {
      /* Original call */
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc);
    } else {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, true);
      // ..., true
      mv.visitLdcInsn(sourceFileName);
      // ..., true, fileName
      mv.visitLdcInsn(methodName);
      // ..., true, fileName, methodName
      ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
      // ..., true, fileName, methodName, inClass
      ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
      // ..., true, fileName, methodName, inClass, line 
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
          FlashlightNames.CONSTRUCTOR_CALL,
          FlashlightNames.CONSTRUCTOR_CALL_SIGNATURE);

      final Label start = new Label();
      final Label end = new Label();
      final Label handler = new Label();
      final Label resume = new Label();
      mv.visitTryCatchBlock(start, end, handler, null);
      
      /* Original call */
      mv.visitLabel(start);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc);
      mv.visitLabel(end);

      /* Normal return */
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., true
      mv.visitLdcInsn(sourceFileName);
      // ..., true, fileName
      mv.visitLdcInsn(methodName);
      // ..., true, fileName, methodName
      ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
      // ..., true, fileName, methodName, inClass
      ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
      // ..., true, fileName, methodName, inClass, line 
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
          FlashlightNames.CONSTRUCTOR_CALL,
          FlashlightNames.CONSTRUCTOR_CALL_SIGNATURE);
      // ...
      mv.visitJumpInsn(Opcodes.GOTO, resume);
      
      /* exception handler */
      mv.visitLabel(handler);
      // ex
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ex, true
      mv.visitLdcInsn(sourceFileName);
      // ex, true, fileName
      mv.visitLdcInsn(methodName);
      // ex, true, fileName, methodName
      ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
      // ex, true, fileName, methodName, inClass
      ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
      // ex, true, fileName, methodName, inClass, line 
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
          FlashlightNames.CONSTRUCTOR_CALL,
          FlashlightNames.CONSTRUCTOR_CALL_SIGNATURE);
      // rethrow exception
      mv.visitInsn(Opcodes.ATHROW);
      
      // resume
      mv.visitLabel(resume);
      
      updateStackDepthDelta(6);
    }
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
    /* If we are in a constructor and we have not yet been initialized then
     * don't instrument the field because we cannot pass the receiver to the
     * Store in an uninitialized state. (The constructors for inner classes have
     * the inits of the "this$" and "val$" fields before the super constructor
     * call.) It's not a big deal that we have missing instrumentation for these
     * fields because they are introduced by the compiler and are not directly
     * accessible by the programmer.
     */
    if (isConstructor && stateMachine != null) {
      mv.visitFieldInsn(Opcodes.PUTFIELD, owner, name, desc);
      return;
    }
        
    final String fullyQualifiedOwner = ByteCodeUtils.internal2FullyQualified(owner);
    final int stackDelta;
    
    /* We need to manipulate the stack to make a copy of the object being
     * accessed so that we can have it for the call to the Store.
     * How we do this depends on whether the top value on the stack is a
     * catagory 1 or a category 2 value.  We have to test the type descriptor
     * of the field to determine this.
     */
    if (ByteCodeUtils.isCategory2(desc)) {
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
    final String fullyQualifiedOwner = ByteCodeUtils.internal2FullyQualified(owner);
//    if (isEnclosingThisField(name)) {
//      /* Don't instrument enclosing this references */
//      mv.visitFieldInsn(Opcodes.GETFIELD, owner, name, desc);
//      return;
//    }

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
    if (ByteCodeUtils.isCategory2(desc)) {
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
    final String fullyQualifiedOwner = ByteCodeUtils.internal2FullyQualified(owner);
    
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
    updateStackDepthDelta(ByteCodeUtils.isCategory2(desc) ? 3 : 4);
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
    final String fullyQualifiedOwner = ByteCodeUtils.internal2FullyQualified(owner);
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
    
    /* We need to insert the expression
     * "Class.forName(<owner>).getDeclaredField(<name>)" into the code.  This puts
     * the java.lang.reflect.Field object for the accessed field on the stack.
     */
    mv.visitLdcInsn(fullyQualifiedOwner);
    // ..., isRead, receiver, className
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_RUNTIME_SUPPORT, FlashlightNames.GET_CLASS, FlashlightNames.GET_CLASS_SIGNATURE);
    // ..., isRead, receiver, classObj
    mv.visitLdcInsn(name);
    // ..., isRead, receiver, classObj, fieldName
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_RUNTIME_SUPPORT, FlashlightNames.GET_FIELD, FlashlightNames.GET_FIELD_SIGNATURE);
    // Stack is "..., isRead, receiver, Field"
    
//    mv.visitInsn(Opcodes.ACONST_NULL);
    
    /* We need to insert the expression "Class.forName(<current_class>)"
     * to push the java.lang.Class object of the referencing class onto the 
     * stack.
     */
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // Stack is "..., isRead, receiver, Field, inClass"
    
    /* Push the line number of the field access. */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // Stack is "..., isRead, receiver, Field, inClass, LineNumber"
    
    /* We can now call Store.fieldAccess() */
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName, FlashlightNames.FIELD_ACCESS, FlashlightNames.FIELD_ACCESS_SIGNATURE);    
    // Stack is "..."
    
    // Resume
  }

  
  
  // =========================================================================
  // == Rewrite monitor methods
  // =========================================================================

//  private void rewriteMonitorenter() {
//    // ..., obj  
//    
//    /* Store the lock object in a local variable, and put it back on the stack */
//    final int lockObj = lvs.newLocal(Type.getObjectType("java/lang/Object"));
//    mv.visitVarInsn(Opcodes.ASTORE, lockObj);
//    // ...
//    mv.visitVarInsn(Opcodes.ALOAD, lockObj);
//    // ..., obj
//    
////    ByteCodeUtils.pushBooleanConstant(mv, false);
////    ByteCodeUtils.pushBooleanConstant(mv, false);
//    
//    /* Check the object against the receiver, unless the method is static */
//    if (isStatic) {
//      // Static methods do not have a receiver
//      ByteCodeUtils.pushBooleanConstant(mv, false);
//      // ..., obj, false
//    } else {
//      /* Push the lock object and the receiver on the stack */
//      mv.visitVarInsn(Opcodes.ALOAD, lockObj);
//      // ..., obj, obj
//      mv.visitVarInsn(Opcodes.ALOAD, 0);
//      // ..., obj, obj, this
//
//      final Label pushFalse1 = new Label();
//      final Label afterPushIsThis = new Label();
//      mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse1);
//      // ..., obj
//      ByteCodeUtils.pushBooleanConstant(mv, true);
//      // ..., obj, true
//      mv.visitJumpInsn(Opcodes.GOTO, afterPushIsThis);
//      mv.visitLabel(pushFalse1);
//      // ..., obj
//      ByteCodeUtils.pushBooleanConstant(mv, false);
//      // ..., obj, false
//      mv.visitLabel(afterPushIsThis);
//    }
//    // ..., obj, isThis
//    
//    /* Duplicate the lock object and check the object against the class object */
//    mv.visitVarInsn(Opcodes.ALOAD, lockObj);
//    // ..., obj, isThis, obj
//    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
//    // ..., obj, isThis, obj, inClass
//    final Label pushFalse2 = new Label();
//    final Label afterPushIsClass = new Label();
//    mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse2);
//    // ..., obj, isThis
//    ByteCodeUtils.pushBooleanConstant(mv, true);
//    // ..., obj, isThis, true
//    mv.visitJumpInsn(Opcodes.GOTO, afterPushIsClass);
//    mv.visitLabel(pushFalse2);
//    // ..., obj, isThis
//    ByteCodeUtils.pushBooleanConstant(mv, false);
//    // ..., obj, isThis, false
//    mv.visitLabel(afterPushIsClass);
//    // ..., obj, isThis, isClass
//    
//    /* Push the current class and the line number, and call the pre-synchronized method */
//    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
//    // ..., obj, isThis, isClass, inClass
//    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
//    // ..., obj, isThis, isClass, inClass, lineNumber
//    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
//        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION,
//        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
//    // ...
//    
//    /* The original monitor enter call */
//    mv.visitVarInsn(Opcodes.ALOAD, lockObj);
//    // ... obj
//    mv.visitInsn(Opcodes.MONITORENTER);
//    // ...
//    
//    /* Make the post-synchronized call */
//    mv.visitVarInsn(Opcodes.ALOAD, lockObj);
//    // ..., obj
//    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
//    // ...., obj, inClass
//    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
//    // ..., obj, inClass, lineNumber
//    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
//        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION,
//        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
//    // ...
//    
//    /* Resume original instruction stream */
//
//    updateStackDepthDelta(4);
//  }

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
    
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);

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
    
    if (isStatic) {
      // Static methods do not have a receiver
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., obj, inClass, obj,   obj, inClass, obj, false
    } else {
      mv.visitInsn(Opcodes.DUP);
      // ..., obj, inClass, obj,   obj, inClass, obj, obj

      /* Compare the object against "this" */
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // ..., obj, inClass, obj,   obj, inClass, obj, obj, this
      final Label pushFalse1 = new Label();
      final Label afterPushIsThis = new Label();
      mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse1);
      // ..., obj, inClass, obj,   obj, inClass, obj
      ByteCodeUtils.pushBooleanConstant(mv, true);
      mv.visitJumpInsn(Opcodes.GOTO, afterPushIsThis);
      mv.visitLabel(pushFalse1);
      // ..., obj, inClass, obj,   obj, inClass, obj
      ByteCodeUtils.pushBooleanConstant(mv, false);
      mv.visitLabel(afterPushIsThis);
    }
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
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // ..., obj, inClass, obj
    
    /* The original monitor enter call */
    mv.visitInsn(Opcodes.MONITORENTER);
    // ..., obj, inClass
    
    /* Push the 3rd parameter for the post-synchronized call and call it */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., obj, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
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
    
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    
    /* Push the lineNumber and call the Store method. */
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., obj, inClass, lineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
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
      ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
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
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // lockObj, isReceiver, isStatic, inClass
    ByteCodeUtils.pushIntegerConstant(mv, 0);
    // lockObj, isReceiver, isStatic, inClass, 0
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
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
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // lockObj, inClass
    ByteCodeUtils.pushIntegerConstant(mv, 0);
    // lockObj, inClass, 0
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
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
    ByteCodeUtils.pushInClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // ..., lockObj, inClass
    ByteCodeUtils.pushIntegerConstant(mv, currentSrcLine);
    // ..., lockObj, inClass, exitLineNumber
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
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
    if (!inInterface) {
      /* Create the wrapper method information and add it to the list of wrappers */
      final MethodCallWrapper wrapper;
      if (opcode == Opcodes.INVOKESPECIAL) {
        wrapper = new SpecialCallWrapper(owner, name, desc);
      } else if (opcode == Opcodes.INVOKESTATIC){
        wrapper = new StaticCallWrapper(owner, name, desc);
      } else if (opcode == Opcodes.INVOKEINTERFACE) {
        wrapper = new InterfaceCallWrapper(owner, name, desc);
      } else { // virtual call
        wrapper = new VirtualCallWrapper(owner, name, desc);
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
    } else {
      final InPlaceMethodInstrumentation methodCall;
      if (opcode == Opcodes.INVOKESTATIC) {
        methodCall = new InPlaceStaticMethodInstrumentation(
            opcode, owner, name, desc, methodName, currentSrcLine);
      } else {
        methodCall = new InPlaceInstanceMethodInstrumentation(
            opcode, owner, name, desc, methodName, currentSrcLine, lvs);
      }
      
      final MethodCallInstrumenter instrumenter = new MethodCallInstrumenter(
          config, mv, methodCall, atLeastJava5, sourceFileName,
          classBeingAnalyzedInternal);
      methodCall.popReceiverAndArguments(mv);
      instrumenter.instrumentMethodCall();
      
      updateStackDepthDelta(7);
    }
  }
}
