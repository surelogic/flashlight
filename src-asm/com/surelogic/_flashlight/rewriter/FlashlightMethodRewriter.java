package com.surelogic._flashlight.rewriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.surelogic._flashlight.rewriter.ConstructorInitStateMachine.Callback;


/**
 * Class visitor that inserts flashlight instrumentation into a method.
 */
final class FlashlightMethodRewriter implements MethodVisitor {
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
      /*
       * Must initialize the flashlight$phantomObject field before calling th
       * constructorExcecution event because constructorExecution() will may
       * cause getPhantom$Reference() to be called. If we don't have init the
       * field first, then this could return null causing a NullPointerException
       * in the store.
       */
      if (mustImplementIIdObject) {
        if (lastInitOwner != null && !lastInitOwner.equals(classBeingAnalyzedInternal)) {
          initPhantomObjectField();
        }
      }
      if (config.rewriteConstructorExecution) {
        insertConstructorExecutionPrefix();
      }
    }
  }
  
  

  /** The delegate method visitor */
  private final MethodVisitor mv;
  
  /** Configuration information, derived from properties. */
  private final Configuration config;

  /** Messenger for reporting status */
  private final EngineMessenger messenger;
  
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
   * Should the super constructor call be updated from {@code java.lang.Object}
   * to {@code com.surelogic._flashlight.rewriter.runtime.IdObject}.
   */
  private final boolean updateSuperCall;
  
  /**
   * Must the class that contains the method implement the IIdObject interface.
   * If so, we need to update the constructors to initialize the field
   * flashlight$phantomObject.
   */
  private final boolean mustImplementIIdObject;
  
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
   * Label for marking the start of the exception handler used when
   * rewriting constructors and synchronized methods.
   */
  private Label startOfExceptionHandler = null;
  
  /**
   * Label for marking the end of the current try-finally block when rewriting
   * synchronized methods.
   */
  private Label endOfTryBlock = null;
  
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
   * calls inside of methods.
   */
  private LocalVariablesSorter lvs;
  
  /**
   * The class hierarchy and field model used to get unique field identifiers.
   */
  private final ClassAndFieldModel classModel;
  
  /**
   * Refers to a thunk used to insert instructions after the following
   * instruction, if that instruction is a label. Otherwise, the operations are
   * inserted before the next instruction. See
   * {@link #delayForLabel(com.surelogic._flashlight.rewriter.FlashlightMethodRewriter.DelayedOutput)}.
   * Once the thunk has been executed, this is reset to {@code null}.
   */
  private DelayedOutput delayedForLabel = null;

  /**
   * If the previously visited element was an ASTORE instruction, this is set to
   * the index of the local variable stored. Otherwise, it is {@code -1}.
   */
  private int previousStore = -1;
  
  /**
   * If the previously visited element was an ALOAD instruction, this is set to
   * the index of the local variable loaded. Otherwise, it is {@code -1}.
   */
  private int previousLoad = -1;
  
  /**
   * When rewriting a synchronized method to use explicit locking, this 
   * holds the id of the local variable that stores the lock object.  Otherwise,
   * it is {@code -1}.
   */
  private int syncMethodLockVariable = -1;
  
  /**
   * Map from one label to another. The key label is a label we receive as a
   * start label in {@link #visitTryCatchBlock}. The value label is the label
   * we remap it to when we call {@code visitTryCatchBlock} to our delegae
   * method visitor. We do this because the compiled code can reuse these labels
   * for jump points, and we are sensitive to the start of try blocks that
   * follow monitorenter operations. In particular, we insert instrumentation
   * code after the start of the try block. But we don't want the program to
   * later jump to start of the try block and reexecute our instrumentation
   * code. First, this is wrong. But also, the stack isn't set up properly for
   * this to work and the bytecode verifier rejects the code.  So we insert a new
   * label for the start of the try block, and insert the original label after
   * our instrumentation code.
   */
  private final Map<Label, Label> tryLabelMap = new HashMap<Label, Label>();
  
  /**
   * Factory for creating unique site identifiers.
   */
  private final SiteIdFactory siteIdFactory;
  
  /**
   * The current site identifier
   */
  private long siteId = 0;
  
  /**
   * The owner of the last "&lt;init&gt;" method called.  Used by the object
   * init callback to determine if the flashlight$phantomObject field should
   * be initialized.  If the last owner is the class being instrumented,
   * then the field is not initialized because we have a "this(...)" call,
   * which would have already initialized the field.
   */
  private String lastInitOwner = null;
  
  
  
  /**
   * Factory method for generating a new instance.  We need this so we can
   * manage the local variables sorter used by the instance.
   */
  public static MethodVisitor create(final int access, final String mname,
      final String desc, final MethodVisitor mv, final Configuration conf,
      final SiteIdFactory csif, final EngineMessenger msg,
      final ClassAndFieldModel model, final boolean java5, final boolean inInt,
      final boolean update, final boolean mustImpl, final String fname,
      final String nameInternal, final String nameFullyQualified,
      final Set<MethodCallWrapper> wrappers) {
    final FlashlightMethodRewriter methodRewriter =
      new FlashlightMethodRewriter(access, mname, desc, mv, conf, csif, msg,
          model, java5, inInt, update, mustImpl, fname, nameInternal, nameFullyQualified,
          wrappers);
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
  private FlashlightMethodRewriter(final int access, final String mname,
      final String desc, final MethodVisitor mv, final Configuration conf,
      final SiteIdFactory csif, final EngineMessenger msg,
      final ClassAndFieldModel model, final boolean java5, final boolean inInt,
      final boolean update, final boolean mustImpl, final String fname,
      final String nameInternal, final String nameFullyQualified,
      final Set<MethodCallWrapper> wrappers) {
    this.mv = mv;
    config = conf;
    siteIdFactory = csif;
    messenger = msg;
    classModel = model;
    atLeastJava5 = java5;
    inInterface = inInt;
    updateSuperCall = update;
    mustImplementIIdObject = mustImpl;
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
    
    /* Reset the site factory for a new method */
    siteIdFactory.setMethodLocation(
        sourceFileName, classBeingAnalyzedFullyQualified, methodName);
  }
  
  
  
  public void visitCode() {
    /* We are just about to start visiting instructions, so we cannot have any
     * delayed instructions yet.
     */
    mv.visitCode();
    
    /* Initialize the site identifier in case the class doesn't have line 
     * number information
     */
    updateSiteIdentifier();
    
    // Initialize the flashlight$withinClass field
    if (isClassInitializer) {
      insertClassInitializerCode();
    }
    if (wasSynchronized && config.rewriteSynchronizedMethod) {
      insertSynchronizedMethodPrefix();
    }
  }
  
  public void visitLineNumber(final int line, final Label start) {
    /* This callback does not correspond to an instruction, so don't worry
     * about delayed instructions.  May affect the line number of inserted 
     * instructions.
     */
    mv.visitLineNumber(line, start);
    currentSrcLine = line;
    updateSiteIdentifier();
  }
  
  public void visitTypeInsn(final int opcode, final String type) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    mv.visitTypeInsn(opcode, type);
    if (stateMachine != null) stateMachine.visitTypeInsn(opcode, type);
  }
  
  public void visitInsn(final int opcode) {
    if (opcode == Opcodes.MONITORENTER && config.rewriteMonitorenter) {
      handlePreviousAload();
      // previous store is dealt with in rewriteMonitorenter
      insertDelayedCode();
      rewriteMonitorenter();
    } else if (opcode == Opcodes.MONITOREXIT && config.rewriteMonitorexit) {
      // previous load is dealt with in rewriteMonitorexit()
      handlePreviousAstore();
      insertDelayedCode();
      rewriteMonitorexit();
    } else if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
      handlePreviousAload();
      handlePreviousAstore();
      insertDelayedCode();
      if (wasSynchronized && config.rewriteSynchronizedMethod) {
        insertSynchronizedMethodExit();
      }
      if (isConstructor && config.rewriteConstructorExecution) {
        insertConstructorExecution(false);
        // Max Stack height is already updated because insertConstructorExecutionPrefix() must have been run 
      }
      mv.visitInsn(opcode);
      
      if (wasSynchronized && config.rewriteSynchronizedMethod) {
        /* Start a new try-block */
        final Label startOfTryBlock = new Label();
        endOfTryBlock = new Label();
        mv.visitTryCatchBlock(startOfTryBlock, endOfTryBlock, startOfExceptionHandler, null);
        mv.visitLabel(startOfTryBlock);
      }
    } else {
      handlePreviousAload();
      handlePreviousAstore();
      insertDelayedCode();
      mv.visitInsn(opcode);
    }

    if (stateMachine != null) stateMachine.visitInsn(opcode);
  }
  
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
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

  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (opcode == Opcodes.INVOKEVIRTUAL) {
      if (config.rewriteInvokevirtual) {
        rewriteMethodCall(Opcodes.INVOKEVIRTUAL, owner, name, desc);
      } else {
        mv.visitMethodInsn(opcode, owner, name, desc);
      }
    } else if (opcode == Opcodes.INVOKESPECIAL) {
      boolean outputOriginalCall = true;
      if (config.rewriteInvokespecial) {
        if (!name.equals(FlashlightNames.CONSTRUCTOR)) {
          outputOriginalCall = false;
          rewriteMethodCall(Opcodes.INVOKESPECIAL, owner, name, desc);
        } else {
          lastInitOwner = owner;
          if (config.rewriteInit) {
            outputOriginalCall = false;
            rewriteConstructorCall(owner, name, desc);
          } else {
            if (isConstructor && stateMachine != null) {
              outputOriginalCall = false;
              updateSuperCall(owner, name, desc);
            }
          }
        }
      } else {
        if (name.equals(FlashlightNames.CONSTRUCTOR)) {
          lastInitOwner = owner;
          if (isConstructor && stateMachine != null) {
            outputOriginalCall = false;
            updateSuperCall(owner, name, desc);
          }
        }
      }      
      if (outputOriginalCall) {
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc);
      }      
    } else if (opcode == Opcodes.INVOKEINTERFACE) {
      if (config.rewriteInvokeinterface) {
        rewriteMethodCall(Opcodes.INVOKEINTERFACE, owner, name, desc);
      } else {
        mv.visitMethodInsn(opcode, owner, name, desc);
      }
    } else if (opcode == Opcodes.INVOKESTATIC) {
      if (config.rewriteInvokestatic) {
        rewriteMethodCall(Opcodes.INVOKESTATIC, owner, name, desc);
      } else {
        mv.visitMethodInsn(opcode, owner, name, desc);
      }
    } else { // Unknown, but safe
      mv.visitMethodInsn(opcode, owner, name, desc);
    }

    if (stateMachine != null) stateMachine.visitMethodInsn(opcode, owner, name, desc);
  }
  
  public void visitMaxs(final int maxStack, final int maxLocals) {
    /* End of instructions, see if we have any last delayed instruction in
     * insert.
     */
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (wasSynchronized && config.rewriteSynchronizedMethod) {
      insertSynchronizedMethodPostfix();
    }
    
    if (isConstructor && config.rewriteConstructorExecution) {
      insertConstructorExecutionPostfix();
    }
    
    mv.visitMaxs(maxStack + stackDepthDelta, maxLocals);
  }

  public void visitIntInsn(final int opcode, final int operand) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitIntInsn(opcode, operand);
    mv.visitIntInsn(opcode, operand);
  }
  
  public void visitJumpInsn(final int opcode, final Label label) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitJumpInsn(opcode, label);
    mv.visitJumpInsn(opcode, label);
  }
  
  public void visitLabel(final Label label) {
    handlePreviousAload();
    handlePreviousAstore();
    if (stateMachine != null) stateMachine.visitLabel(label);
    
    /* If this label is the start of a try-block, output our new start label
     * instead.  
     * 
     * We output the original label below after handling any delayed output.  That
     * is, the original label follows any instrumentation we might insert.
     */
    final Label newLabel = tryLabelMap.get(label);
    mv.visitLabel((newLabel != null) ? newLabel : label);
    insertDelayedCode();
    if (newLabel != null) {
      mv.visitLabel(label);
    }
  }
  
  public void visitLdcInsn(final Object cst) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitLdcInsn(cst);
    mv.visitLdcInsn(cst);
  }
  
  public void visitLookupSwitchInsn(
      final Label dflt, final int[] keys, final Label[] labels) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitLookupSwitchInsn(dflt, keys, labels);
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }
  
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitMultiANewArrayInsn(desc, dims);
    mv.visitMultiANewArrayInsn(desc, dims);
  }
  
  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label[] labels) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitTableSwitchInsn(min, max, dflt, labels);
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }
  
  public void visitVarInsn(final int opcode, final int var) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    if (stateMachine != null) stateMachine.visitVarInsn(opcode, var);
    if (opcode == Opcodes.ASTORE) {
      previousStore = var;
    } else if (opcode == Opcodes.ALOAD) {
      previousLoad = var;
    } else {
      mv.visitVarInsn(opcode, var);
    }
  }

  public AnnotationVisitor visitAnnotationDefault() {
    /* Called before visitCode(), so we don't have worry about inserting any
     * delayed instructions.
     */
    return mv.visitAnnotationDefault();
  }

  public AnnotationVisitor visitAnnotation(final String desc,
      final boolean visible) {
    /* Called before visitCode(), so we don't have worry about inserting any
     * delayed instructions.
     */
    return mv.visitAnnotation(desc, visible);
  }

  public AnnotationVisitor visitParameterAnnotation(final int parameter,
      final String desc, final boolean visible) {
    /* Called before visitCode(), so we don't have worry about inserting any
     * delayed instructions.
     */
    return mv.visitParameterAnnotation(parameter, desc, visible);
  }

  public void visitAttribute(final Attribute attr) {
    /* Called before visitCode(), so we don't have worry about inserting any
     * delayed instructions.
     */
    mv.visitAttribute(attr);
  }

  public void visitFrame(final int type, final int nLocal,
      final Object[] local, final int nStack, final Object[] stack) {
    /* This callback doesn't represent a bytecode instruction, so we don't
     * do anything about delayed instructions yet.
     */
    mv.visitFrame(type, nLocal, local, nStack, stack);
  }

  public void visitIincInsn(final int var, final int increment) {
    handlePreviousAload();
    handlePreviousAstore();
    insertDelayedCode();
    mv.visitIincInsn(var, increment);
  }

  public void visitTryCatchBlock(final Label start, final Label end,
      final Label handler, final String type) {
    /* This callback doesn't represent a bytecode instruction, so we don't
     * do anything about delayed instructions yet.
     */
    
    /* Remap the label for the start of the try block.  But only if we haven't
     * seen it before.
     */
    Label newStart = tryLabelMap.get(start);
    if (newStart == null) {
      newStart = new Label();
      tryLabelMap.put(start, newStart);
    }
    mv.visitTryCatchBlock(newStart, end, handler, type);
  }

  public void visitLocalVariable(final String name, final String desc,
      final String signature, final Label start, final Label end,
      final int index) {
    /* This callback doesn't represent a bytecode instruction, so we don't
     * do anything about delayed instructions yet.
     */
    mv.visitLocalVariable(name, desc, signature, start, end, index);
  }

  public void visitEnd() {
    /* visitMaxs already cleared out the remaining delayed instructions. */
    mv.visitEnd();
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
   * Abstract class for holding instrumentation code that needs executed after
   * the next instruction, if the next instruction is a label, or before the next
   * instruction if it is not a label.
   */
  private abstract class DelayedOutput {
    /**
     * Called to insert instructions after the triggering instruction has
     * been encountered.
     */
    public abstract void insertCode();
  }
  
  
  private void delayForLabel(final DelayedOutput delayed) {
    if (delayedForLabel != null) {
      throw new IllegalStateException("Already have pending output");
    } else {
      delayedForLabel = delayed;
    }
  }
  
  /**
   * Called in one of two ways:
   * <ul>
   * <li>Called after we have visited a label and called {@code visitLabel}
   * on the method visitor delegate.
   * <li>Called when we are starting to visit a non-label instruction, before we
   * have called {@code visit...} on the method visitor delegate.
   * </ul>
   */
  private void insertDelayedCode() {
    if (delayedForLabel != null) {
      delayedForLabel.insertCode();
      delayedForLabel = null;
    }
  }

  
  /**
   * For monitorexit operations we want to know if the previous instruction 
   * was an aload.  If so, we want to make sure we don't insert any instructions
   * between the ALOAD and the MONITOREXIT, or the JIT compiler will not compile
   * the method to native code.  So when we encounter an ALOAD in
   * {@link #visitVarInsn} we record the variable id, but do not forward the 
   * ALOAD to the method visitor delegate. When we visit an operation other than
   * monitorexit, we call this method first to make sure we forward the ALOAD to
   * the method visitor delegate before we do anything else.  When we visit a
   * monitorexit operation we handle this case specially.
   */
  private void handlePreviousAload() {
    if (previousLoad != -1) {
      mv.visitVarInsn(Opcodes.ALOAD, previousLoad);
      previousLoad = -1;
    }
  }
  
  /**
   * For monitorenter operations we want to know if the previous instruction 
   * was an astore.  If so, we want to make sure we don't insert any instructions
   * between the astore and the monitorenter, or the JIT compiler will not compile
   * the method to native code.  So when we encounter an astore in
   * {@link #visitVarInsn} we record the variable id, but do not forward the 
   * astore to the method visitor delegate. When we visit an operation other than
   * monitorenter, we call this method first to make sure we forward the astore to
   * the method visitor delegate before we do anything else.  When we visit a
   * monitorenter operation we handle this case specially.
   */
  private void handlePreviousAstore() {
    if (previousStore != -1) {
      mv.visitVarInsn(Opcodes.ASTORE, previousStore);
      previousStore = -1;
    }
  }
  
  
  
  // =========================================================================
  // == Insert Bookkeeping code
  // =========================================================================

  private void insertClassInitializerCode() {
    // Stack is empty (we are at the beginning of the method!)
    
    /* Initialize the flashlight$classObject field.  Do this first so that we
     * can use it below to initialize the flashlight$withinClass field.  
     * We only need this field if the classfile version is less than 49.0
     * (pre-Java 5).
     */
    if (!atLeastJava5) {
      /* We need to insert the expression "Class.forName(<fully-qualified-class-name>)"
       * into the code, and set the static field flashlight$inClass
       */
      mv.visitLdcInsn(classBeingAnalyzedFullyQualified);
      // className
      mv.visitMethodInsn(Opcodes.INVOKESTATIC,
          FlashlightNames.JAVA_LANG_CLASS,
          FlashlightNames.FOR_NAME,
          FlashlightNames.FOR_NAME_SIGNATURE);
      // Class
      mv.visitFieldInsn(Opcodes.PUTSTATIC, classBeingAnalyzedInternal,
          FlashlightNames.FLASHLIGHT_CLASS_OBJECT,
          FlashlightNames.FLASHLIGHT_CLASS_OBJECT_DESC);
    }
    // empty stack

    /*
     * Set flashlight$withinClass by calling Store.getClassPhantom() 
     */
    ByteCodeUtils.pushClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // Class
    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
        config.storeClassName,
        FlashlightNames.GET_CLASS_PHANTOM,
        FlashlightNames.GET_CLASS_PHANTOM_SIGNATURE);
    // ClassPhantomReference
    mv.visitFieldInsn(Opcodes.PUTSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC);
    // empty stack
    
    /* Set the static field flashlight$classLoaderInfo */
    ByteCodeUtils.pushClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // Class
    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
        FlashlightNames.FLASHLIGHT_RUNTIME_SUPPORT,
        FlashlightNames.GET_CLASSLOADER_INFO,
        FlashlightNames.GET_CLASSLOADER_INFO_SIGNATURE);
    // ClassLoaderInfo
    mv.visitFieldInsn(Opcodes.PUTSTATIC, classBeingAnalyzedInternal,
        FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO,
        FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO_DESC);    
    
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
    final Label startOfOriginalConstructor = new Label();
    startOfExceptionHandler = new Label();
    mv.visitTryCatchBlock(startOfOriginalConstructor,
        startOfExceptionHandler, startOfExceptionHandler, null);
    
    /* Start of constructor */
    mv.visitLabel(startOfOriginalConstructor);
    
    updateStackDepthDelta(4);
  }
  
  private void insertConstructorExecutionPostfix() {
    mv.visitLabel(startOfExceptionHandler);
    
    // exception 
    insertConstructorExecution(false); // +4 on stack
    // exception
    
    /* Rethrow the exception */
    mv.visitInsn(Opcodes.ATHROW);
    
    updateStackDepthDelta(5);
    
    startOfExceptionHandler = null;
  }
  
  private void insertConstructorExecution(final boolean before) {
    // ...
    ByteCodeUtils.pushBooleanConstant(mv, before);
    // ..., before
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    // ..., before, this
    pushSiteIdentifier();
    // ..., before, this, siteId
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.CONSTRUCTOR_EXECUTION,
        FlashlightNames.CONSTRUCTOR_EXECUTION_SIGNATURE);
    // ...
  }
  
  private void updateSuperCall(
      final String owner, final String name, final String desc) {
    if (owner.equals(FlashlightNames.JAVA_LANG_OBJECT) && updateSuperCall) {
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, FlashlightNames.ID_OBJECT, name, desc);
    } else {
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc);
    }
  }
  
  private void rewriteConstructorCall(
      final String owner, final String name, final String desc) {
    if (isConstructor && stateMachine != null) {
      updateSuperCall(owner, name, desc);
    } else {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, true);
      // ..., true
      pushSiteIdentifier();
      // ..., true, siteId
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
      // ..., false
      pushSiteIdentifier();
      // ..., false, siteId
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
          FlashlightNames.CONSTRUCTOR_CALL,
          FlashlightNames.CONSTRUCTOR_CALL_SIGNATURE);
      // ...
      mv.visitJumpInsn(Opcodes.GOTO, resume);
      
      /* exception handler */
      mv.visitLabel(handler);
      // ex
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ex, false
      pushSiteIdentifier();
      // ex, false, siteId
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
    
    /* We need to manipulate the stack to make a copy of the object being
     * accessed so that we can have it for the call to the Store.
     * How we do this depends on whether the top value on the stack is a
     * catagory 1 or a category 2 value.  We have to test the type descriptor
     * of the field to determine this.
     */
    if (ByteCodeUtils.isCategory2(desc)) {
      // At the start the stack is "..., objectref, value"
      mv.visitInsn(Opcodes.DUP2_X1);
      // Stack is "..., value, objectref, value" (+2)
      mv.visitInsn(Opcodes.POP2);
      // Stack is "..., value, objectref" (+0)
      mv.visitInsn(Opcodes.DUP_X2);
      // Stack is "..., objectref, value, objectref" (+1)
      mv.visitInsn(Opcodes.DUP_X2);
      // Stack is "..., objectref, objectref, value, objectref" (+2)
      mv.visitInsn(Opcodes.POP);
      // Stack is "..., objectref, objectref, value" (+1)      
    } else { // Category 1
      // At the start the stack is "..., objectref, value"
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "..., value, objectref" (+0)
      mv.visitInsn(Opcodes.DUP_X1);
      // Stack is "..., objectref, value, objectref" (+1)
      mv.visitInsn(Opcodes.SWAP);
      // Stack is "..., objectref, objectref, value" (+1)
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
    
    final int pushed = finishFieldAccess(false,
        owner, fullyQualifiedOwner, name,
        FlashlightNames.INSTANCE_FIELD_ACCESS,
        FlashlightNames.INSTANCE_FIELD_ACCESS_SIGNATURE,
        FlashlightNames.INSTANCE_FIELD_ACCESS_LOOKUP,
        FlashlightNames.INSTANCE_FIELD_ACCESS_LOOKUP_SIGNATURE);
    
    // Update stack depth
    updateStackDepthDelta(2 + pushed);
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
    
    final int pushed = finishFieldAccess(false,
        owner, fullyQualifiedOwner, name,
        FlashlightNames.INSTANCE_FIELD_ACCESS,
        FlashlightNames.INSTANCE_FIELD_ACCESS_SIGNATURE,
        FlashlightNames.INSTANCE_FIELD_ACCESS_LOOKUP,
        FlashlightNames.INSTANCE_FIELD_ACCESS_LOOKUP_SIGNATURE);
    
    /* Update stack depth: Works because pushed >= 1.  We need the stack depth
     * to increase by at least 3 in the case of value being category 2.
     */
    updateStackDepthDelta(2+pushed);
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
    
    /* Push the first arguments on the stack for the call to the
     * Store.
     */
    ByteCodeUtils.pushBooleanConstant(mv, false);
    // Stack is "..., false"
    
    final int pushed = finishFieldAccess(true,
        owner, fullyQualifiedOwner, name,
        FlashlightNames.STATIC_FIELD_ACCESS,
        FlashlightNames.STATIC_FIELD_ACCESS_SIGNATURE,
        FlashlightNames.STATIC_FIELD_ACCESS_LOOKUP,
        FlashlightNames.STATIC_FIELD_ACCESS_LOOKUP_SIGNATURE);
    
    /* Update stack depth. Depends on the category of the original value on the
     * stack.
     */
    updateStackDepthDelta((ByteCodeUtils.isCategory2(desc) ? -1 : 0) + pushed);
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
    
    /* Manipulate the stack so that we push the first argument to 
     * the Store method.
     */
    ByteCodeUtils.pushBooleanConstant(mv, true);
    // Stack is "..., value, true"
    
    final int pushed = finishFieldAccess(true,
        owner, fullyQualifiedOwner, name,
        FlashlightNames.STATIC_FIELD_ACCESS,
        FlashlightNames.STATIC_FIELD_ACCESS_SIGNATURE,
        FlashlightNames.STATIC_FIELD_ACCESS_LOOKUP,
        FlashlightNames.STATIC_FIELD_ACCESS_LOOKUP_SIGNATURE);
    
    // Update stack depth
    updateStackDepthDelta(1+pushed);
  }



  /**
   * All the field access rewrites end in the same general way once the "isRead"
   * and "receiver" (if any) parameters are pushed onto the stack. This pushes
   * the rest of the parameters on the stack and introduces the call to the
   * appropriate Store method.
   * 
   * <p>
   * When called for an instance field the stack should be "..., isRead,
   * receiver". When called for a static field, the stack should be "...,
   * isRead".
   * 
   * @param name
   *          The name of the field being accessed.
   * @param fullyQualifiedOwner
   *          The fully qualified class name of the class that declares the
   *          field being accessed.
   * @param foundMethodName
   *          The name of the Store method to call if we found the field's
   *          unique identifier.
   * @param foundMethodSignature
   *          The signature of the Store method to call if we found the field's
   *          unique identifier.
   * @param lookupMethodName
   *          The name of the Store method to call if we didn't find the field's
   *          unique identifier and need to use reflection.
   * @param lookupMethodSignature
   *          The signature of the Store method to call if we didn't find the
   *          field's unique identifier and need to use reflection.
   * @return The number of items pushed on the stack
   */
  private int finishFieldAccess(final boolean isStatic,
      final String owner, final String fullyQualifiedOwner, final String name,
      final String foundMethodName, final String foundMethodSignature,
      final String lookupMethodName, final String lookupMethodSignature) {
    // Stack is "..., isRead, [receiver]"

    final String storeMethodName;
    final String storeMethodSignature;
    final int pushed;
    final Integer fid = classModel.getFieldID(fullyQualifiedOwner, name);
    if (fid != ClassAndFieldModel.FIELD_NOT_FOUND) {
      if (isStatic) {
        ByteCodeUtils.pushPhantomClass(mv, owner);
        pushed = 4;
      } else {
        pushed = 3;
      }
      /* Push the id of the field */
      mv.visitLdcInsn(fid);
      // Stack is "..., isRead, [receiver/ownerPhantom], field_id
      
      storeMethodName = foundMethodName;
      storeMethodSignature = foundMethodSignature;
    } else {
      messenger.verbose("Field " + fullyQualifiedOwner + "." + name
          + " needs reflection at " + classBeingAnalyzedFullyQualified + "."
          + methodName + "():" + currentSrcLine);

      // Stack is "..., isRead, [receiver]"
  
      /* Push the class object for the owner class.  We get the class object
       * by invoking flashlight$classLoaderInfo.getClass().
       */
      mv.visitFieldInsn(Opcodes.GETSTATIC, classBeingAnalyzedInternal,
          FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO,
          FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO_DESC);
      // ..., isRead, [receiver], classLoaderInfo
      mv.visitLdcInsn(fullyQualifiedOwner);
      // ..., isRead, [receiver], classLoaderInfo, className
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
          FlashlightNames.CLASS_LOADER_INFO,
          FlashlightNames.GET_CLASS,
          FlashlightNames.GET_CLASS_SIGNATURE);
      // ..., isRead, [receiver], class object
      
      /* Push the field name */
      mv.visitLdcInsn(name);
      // ..., isRead, [receiver], class object, fieldName
      
      storeMethodName = lookupMethodName;
      storeMethodSignature = lookupMethodSignature;
      pushed = 4;
    }
    
    // Stack is "..., isRead, [receiver/ownerPhantom], field_id" or
    // "..., isRead, [receiver], class object, fieldName"
    
    /* Push the site identifer */
    pushSiteIdentifier();
    // Stack is "..., isRead, [receiver/ownerPhantom], field_id, siteId" or
    // "..., isRead, [receiver], class object, fieldName, siteId"
        
    /* We can now call the store method */
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        storeMethodName, storeMethodSignature);    
    // Stack is "..."

    // Resume
    return pushed;
  }

  
  
  // =========================================================================
  // == Rewrite monitor methods
  // =========================================================================

  private void rewriteMonitorenter() {
    if (previousStore != -1) {
      /* There was an ASTORE immediately preceding this monitorenter.  We want
       * to delay the output of the ASTORE until immediately before the monitorenter
       * that we output.  In this case the stack is already "..., obj, obj"
       */
      // ..., obj, obj
    } else {
      /* Copy the lock object to use for comparison purposes */
      mv.visitInsn(Opcodes.DUP);
      // ..., obj, obj
    }
    
    /* Compare the lock object against the receiver */
    if (isStatic) {
      // Static methods do not have a receiver
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., obj, obj, false
    } else {
      mv.visitInsn(Opcodes.DUP);
      // ..., obj, obj, obj

      /* Compare the object against "this" */
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      // ..., obj, obj, obj, this
      final Label pushFalse1 = new Label();
      final Label afterPushIsThis = new Label();
      mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse1);
      // ..., obj, obj
      ByteCodeUtils.pushBooleanConstant(mv, true);
      // ..., obj, obj, true
      mv.visitJumpInsn(Opcodes.GOTO, afterPushIsThis);
      // END
      mv.visitLabel(pushFalse1);
      // ..., obj, obj
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., obj, obj, false
      mv.visitLabel(afterPushIsThis);
    }
    // ..., obj, obj, isThis

    /* Compare the object being locked against the Class object */
    mv.visitInsn(Opcodes.SWAP);
    // ..., obj, isThis, obj
    mv.visitInsn(Opcodes.DUP_X1);
    // ..., obj, obj, isThis, obj
    ByteCodeUtils.pushClass(mv, atLeastJava5, classBeingAnalyzedInternal);
    // ..., obj, obj, isThis, obj, inClass
    final Label pushFalse2 = new Label();
    final Label afterPushIsClass = new Label();
    mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse2);
    // ..., obj, obj, isThis
    ByteCodeUtils.pushBooleanConstant(mv, true);
    // ..., obj, obj, isThis, true
    mv.visitJumpInsn(Opcodes.GOTO, afterPushIsClass);
    // END
    mv.visitLabel(pushFalse2);
    // ..., obj, obj, isThis
    ByteCodeUtils.pushBooleanConstant(mv, false);
    // ..., obj, obj, isThis, false
    mv.visitLabel(afterPushIsClass);
    // ..., obj, obj, isThis, isClass

    /* Push the site identifier and call the pre-method */
    pushSiteIdentifier();
    // ..., obj, obj, isThis, isClass, siteId
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // ..., obj

    /* Duplicate the lock object to have it for the post-synchronized method */
    mv.visitInsn(Opcodes.DUP);
    // ..., obj, obj
    
    if (previousStore != -1) {
      /* Duplicate again, and store it in the local variable */
      mv.visitInsn(Opcodes.DUP);
      // ..., obj, obj, obj
      
      mv.visitVarInsn(Opcodes.ASTORE, previousStore);
      previousStore = -1;
      // ..., obj, obj
    }
    
    // ..., obj, obj      
      
    /* The original monitor enter call */
    mv.visitInsn(Opcodes.MONITORENTER);
    // ..., obj
    
    /* To make the JIT compiler happy, we must start the try-block immediately
     * after the monitorenter operation.  This means we must delay the
     * insertion of our operations to call the post-monitorenter logging call
     * until after the label that follows the monitorenter.
     */
    /* Save the original site identifier, in case it changes before the delayed
     * code is output.
     */
    final long originalSiteId = siteId;
    delayForLabel(new DelayedOutput() {
      @Override
      public void insertCode() {
        /* Push the site identifier and call the post-method */
        pushSiteIdentifier(originalSiteId);
        // ..., obj, siteId
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
            FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION,
            FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
        // ...
      }
    });
    
    /* Resume original instruction stream */

    updateStackDepthDelta(5);
  }

  private void rewriteMonitorexit() {
    if (previousLoad != -1) {
      // ...
      
      /* There was an ALOAD immediately preceding this monitorexit, but
       * we haven't output it yet.  We need two copies of the object, one
       * for the monitorexit, and one for our post-call that follows it, so 
       * we need to insert the ALOAD twice.  The point here is to make sure that
       * the ALOAD still immediately precedes the monitorexit.
       */  
      mv.visitVarInsn(Opcodes.ALOAD, previousLoad);
      // ..., obj
      mv.visitVarInsn(Opcodes.ALOAD, previousLoad);
      // ..., obj, obj
      
      previousLoad = -1;
    } else {
      // ..., obj  
      
      /* Copy the object being locked for use as the first parameter to
       * Store.afterInstrinsicLockRelease().
       */
      mv.visitInsn(Opcodes.DUP);
      // ..., obj, obj  
    }
    /* The original monitor exit call */
    mv.visitInsn(Opcodes.MONITOREXIT);
    // ..., obj
    
    /* To make the JIT compiler happy, we must terminate the try-block
     * just after the monitorexit operation.  This means we must delay the insertion
     * of the post-monitorexit logging method until after the label that follows the 
     * monitorexit instruction is output.
     */
    /* Save the original site id in case the line number changes before the
     * delayed code is output.
     */
    final long originalSiteId = siteId;
    delayForLabel(new DelayedOutput() {
      @Override
      public void insertCode() {
        pushSiteIdentifier(originalSiteId);
        // ..., obj, siteId
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
            FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE, FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE);
        // ...
      }
    });      

    /* Resume original instruction stream */
    updateStackDepthDelta(2);
  }
  
  
  // =========================================================================
  // == Rewrite synchronized methods
  // =========================================================================
  
  private void pushSynchronizedMethodLockObject() {
    if (isStatic) {
      ByteCodeUtils.pushClass(mv, atLeastJava5, classBeingAnalyzedInternal);
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
    pushSiteIdentifier();
    // lockObj, isReceiver, isStatic, siteid
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // empty stack
    
    /* Insert the explicit monitor acquisition.  Even though we already know
     * the lock object by context, we need to store it in a local variable 
     * so that we emulate the bytecode produced by the Java compiler.  If we 
     * don't do this, then the JIT compiler will not compiler the instrumented
     * method to native code.  Also, our try-block must start immediately after
     * the monitorenter, and end immediately after the normal monitorexit.
     */
    pushSynchronizedMethodLockObject();
    // lockObj
    mv.visitInsn(Opcodes.DUP);
    // lockObj, lockObj
    syncMethodLockVariable = lvs.newLocal(Type.getObjectType("java/lang/Object"));
    mv.visitVarInsn(Opcodes.ASTORE, syncMethodLockVariable);
    // lockObj
    mv.visitInsn(Opcodes.MONITORENTER);
    // empty stack

    final Label startOfTryBlock = new Label();
    endOfTryBlock = new Label();
    startOfExceptionHandler = new Label();
    mv.visitTryCatchBlock(startOfTryBlock, endOfTryBlock, startOfExceptionHandler, null);
    mv.visitLabel(startOfTryBlock);

    /* Now call Store.afterIntrinsicLockAcquisition */
    pushSynchronizedMethodLockObject();
    // lockObj
    pushSiteIdentifier();
    // lockObj, siteId
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION,
        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION_SIGNATURE);
    // empty stack
    
    // Resume code
    
    updateStackDepthDelta(5);
  }
  
  private void insertSynchronizedMethodPostfix() {
    /* The exception handler also needs an exception handler.  A new handler
     * was already started following the last return, and because we are at the
     * end of the method, and all methods must return, this code is part of
     * that handler.  The try-block for the handler itself is closed in
     * insertSynchronizedMethodExit().
     */
    
    /* The exception handler */
    mv.visitLabel(startOfExceptionHandler);
    
    // exception 
    insertSynchronizedMethodExit();
    // exception
    
    /* Rethrow the exception */
    mv.visitInsn(Opcodes.ATHROW);
    
    /* Should update the stack depth, but we know we only get executed if
     * insertSynchronizedMethodPrefix() is run, and that already updates the
     * stack depth by 5, which is more than we need here (4).
     */ 
    
    endOfTryBlock = null;
    startOfExceptionHandler = null;
    syncMethodLockVariable = -1;
  }

  private void insertSynchronizedMethodExit() {
    // ...
    
    /* Explicitly release the lock */
    mv.visitVarInsn(Opcodes.ALOAD, syncMethodLockVariable);
    // ..., lockObj
    mv.visitInsn(Opcodes.MONITOREXIT);
    // ...
    
    /* End of try-block */
    mv.visitLabel(endOfTryBlock);
    
    /* Call Store.afterIntrinsicLockRelease(). */
    pushSynchronizedMethodLockObject();
    // ..., lockObj
    pushSiteIdentifier();
    // ..., lockObj, siteId
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE, FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE_SIGNATURE);
    // ...
    
    // Resume code

    /* Should update the stack depth, but we know we only get executed if
     * insertSynchronizedMethodPrefix() is run, and that already updates the
     * stack depth by 5, which is more than we need here (3).
     */ 
    
    /* New try block is inserted in visitInsn() so that it starts after the
     * return operation.
     */
  }

  
  
  // =========================================================================
  // == Rewrite method calls
  // =========================================================================
  
  private void rewriteMethodCall(final int opcode,
      final String owner, final String name, final String desc) {
  	/* The clone() method is a special case due to its retarded 
  	 * semantics.  If the class of the object being used as the receiver,
  	 * call it C, implements Cloneable, but DOES NOT override the clone() method, 
  	 * the clone method is still seen as a protected method from Object.
  	 * This means, we won't be able to invoke the method from a static
  	 * method in C because the receiver will still be seen as being of
  	 * type Object.  This situation works in JRE 5, but not in JRE 6 due to
  	 * a stricter bytecode verifier.
  	 * 
  	 * The best thing to do in this case is to inline the instrumentation.
  	 */  
  	final boolean isClone = (opcode == Opcodes.INVOKEVIRTUAL)
  		&& name.equals("clone") && desc.startsWith("()");
    if (!inInterface && !isClone) {
      /* Create the wrapper method information and add it to the list of wrappers */
      final MethodCallWrapper wrapper;
      if (opcode == Opcodes.INVOKESPECIAL) {
        wrapper = new SpecialCallWrapper(siteId, owner, name, desc);
      } else if (opcode == Opcodes.INVOKESTATIC){
        wrapper = new StaticCallWrapper(siteId, owner, name, desc);
      } else if (opcode == Opcodes.INVOKEINTERFACE) {
        wrapper = new InterfaceCallWrapper(siteId, owner, name, desc);
      } else { // virtual call
        wrapper = new VirtualCallWrapper(siteId, owner, name, desc);
      }
      
      wrapperMethods.add(wrapper);
      
      // ..., [objRef], arg1, ..., argN
      wrapper.invokeWrapperMethod(mv, classBeingAnalyzedInternal);
      // ..., [returnVlaue]
      
      updateStackDepthDelta(2);
    } else {
      final InPlaceMethodInstrumentation methodCall;
      if (opcode == Opcodes.INVOKESTATIC) {
        methodCall = new InPlaceStaticMethodInstrumentation(siteId, 
            opcode, owner, name, desc);
      } else {
        methodCall = new InPlaceInstanceMethodInstrumentation(siteId, 
            opcode, owner, name, desc, lvs);
      }
      
      final MethodCallInstrumenter instrumenter =
        new MethodCallInstrumenter(config, mv, methodCall);
      methodCall.popReceiverAndArguments(mv);
      instrumenter.instrumentMethodCall();
      
      updateStackDepthDelta(7);
    }
  }

  
  
  // =========================================================================
  // == For implementing IIdObject
  // =========================================================================

  // +2 on the stack
  private void initPhantomObjectField() {
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.ID_OBJECT,
        FlashlightNames.GET_NEW_ID, FlashlightNames.GET_NEW_ID_SIGNATURE);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, config.storeClassName,
        FlashlightNames.GET_OBJECT_PHANTOM,
        FlashlightNames.GET_OBJECT_PHANTOM_SIGNATURE);
    mv.visitFieldInsn(Opcodes.PUTFIELD, classBeingAnalyzedInternal,
        FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT,
        FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT_DESC);
  }
  
  
 
  private void updateSiteIdentifier() {
    siteId = siteIdFactory.getSiteId(currentSrcLine);
  }
  
  private void pushSiteIdentifier(final long id) {
    mv.visitLdcInsn(Long.valueOf(id));
  }
  
  private void pushSiteIdentifier() {
    pushSiteIdentifier(siteId);
  }
}
