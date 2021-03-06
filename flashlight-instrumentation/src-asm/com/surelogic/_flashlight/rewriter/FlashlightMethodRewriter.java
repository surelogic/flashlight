package com.surelogic._flashlight.rewriter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.surelogic._flashlight.Store;
import com.surelogic._flashlight.common.HappensBeforeConfig.HBType;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeCollectionRule;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeExecutorRule;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeObjectRule;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeRule;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeSwitch;
import com.surelogic._flashlight.common.HappensBeforeConfig.ReturnCheck;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.Field;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.FieldNotFoundException;
import com.surelogic._flashlight.rewriter.ConstructorInitStateMachine.Callback;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.Configuration.FieldFilter;

/**
 * Class visitor that inserts flashlight instrumentation into a method.
 */

final class FlashlightMethodRewriter extends MethodVisitor implements
        LocalVariableGenerator {
    private static final String MISSING_CLASS_MESSAGE = "The Flashlight class model is missing class {0} because the classpath provided during instrumentation is incomplete or incorrect.  If the same classpath is provided at runtime, then the application would throw java.lang.NoClassDefFoundError.";
    private static final String MISSING_FIELD_MESSAGE = "The Flashlight class model is missing field {0} in class {1} because the classpath provided during instrumentation is incomplete or incorrect.  if the same classpath is provided at runtime, then the application would throw java.lang.NoSuchFieldError.";

    private static final String CLASS_INITIALIZER = "<clinit>";
    private static final String INITIALIZER = "<init>";

    /**
     * Call back for the constructor initialization state machine. Clears the
     * machine, and inserts the constructor execution begin event into the
     * constructor code.
     */
    private final class ObjectInitCallback implements Callback {
        @Override
        @SuppressWarnings("synthetic-access")
        public void superConstructorCalled() {
            stateMachine = null;
            /*
             * Must initialize the flashlight$phantomObject field before calling
             * the constructorExcecution event because constructorExecution()
             * may cause getPhantom$Reference() to be called. If we don't init
             * the field first, then this could return null causing a
             * NullPointerException in the store.
             */
            if (mustImplementIIdObject) {
                if (lastInitOwner != null
                        && !lastInitOwner.equals(classBeingAnalyzedInternal)) {
                    initPhantomObjectField();
                }
            }
            if (config.rewriteConstructorExecution) {
                insertConstructorExecutionPrefix();
            }
        }
    }
    
    private final static class CallInRecord {
      private HappensBeforeRule happensBefore;
      private int argumentLocal;
      
      public CallInRecord(final LocalVariableGenerator lvg, final HappensBeforeRule hb) {
        happensBefore = hb;
        argumentLocal = lvg.newLocal(Type.getObjectType("java/lang/Object"));
      }
      
      public int getLocal() {
        return argumentLocal;
      }
      
      public Integer getArgument() {
        if (happensBefore instanceof HappensBeforeCollectionRule) {
          return ((HappensBeforeCollectionRule) happensBefore).getObjectParam();
        } else {
          return null;
        }
      }
      
      public ReturnCheck getReturnCheck() {
        return happensBefore.getReturnCheck();
      }
      
      public boolean isSource() {
        return happensBefore.getType() == HBType.SOURCE;
      }
      
      public void invokeSwitch(final HappensBeforeSwitch hbSwitch) {
        happensBefore.invokeSwitch(hbSwitch);
      }
      
      @Override
      public String toString() {
        return "<" + happensBefore + ", " + argumentLocal + ">";
      }
    }
    
    private final static class CallInRecords implements Iterable<CallInRecord> {
      private final List<CallInRecord> records = new ArrayList<CallInRecord>();
      private final boolean needsSource ;
      private final boolean needsTarget;

      public CallInRecords(
          final LocalVariableGenerator lvg, final Set<HappensBeforeRule> rules) {
        boolean source = false;
        boolean target = false;
        for (final HappensBeforeRule hb : rules) {
          records.add(new CallInRecord(lvg, hb));
          final HBType type = hb.getType();
          source |= (type == HBType.SOURCE);
          target |= (type == HBType.TARGET);
        }
        needsSource = source;
        needsTarget = target;
      }
      
      public Iterator<CallInRecord> iterator() { return records.iterator(); }
      public boolean hasSource() { return needsSource; }
      public boolean hasTarget() { return needsTarget; }
      
      @Override
      public String toString() {
        return "<" + needsSource + ", " + needsTarget + ", " + records + ">";
      }
    }

    /**
     * Purposely shadow <code>mv</code> field from {@link MethodVisitor} so that
     * we can refer to the actual type of the delegated visitor:
     * {@link ExceptionHandlerReorderingMethodAdapter}.
     */
    final ExceptionHandlerReorderingMethodAdapter mv;

    /**
     * The current instruction from the MethodNode model of the method.
     * Initialized from the InsnList of the MethodNode. We use this to peek at
     * any annotations associated with an instruction. Must not be
     * <code>null</code> unless we ran out of instructions, which should only
     * happen at the end of the method.
     */
    AbstractInsnNode currentInsn;

    /** Configuration information, derived from properties. */
    final Configuration config;

    /** The happens-before method data base */
    final HappensBeforeTable happensBefore;

    /** Messenger for reporting status */
    final RewriteMessenger messenger;

    /** Is the current classfile an interface? */
    final boolean inInterface;

    /** The internal name of the class being rewritten. */
    final String classBeingAnalyzedInternal;

    /**
     * The internal package name of the class being rewritten. Derived from
     * {@link #classBeingAnalyzedInternal}, but cached as a separate field so we
     * don't have to recomputed it for every field access.
     */
    final String packageNameInternal;

    /** The internal name of the superclass of the class being rewritten. */
    final String superClassInternal;

    /** The simple name of the method being rewritten. */
    final String methodName;

    /** The descriptor of the method being rewritten. */
    final String methodDesc;

    /** Are we visiting a constructor? */
    final boolean isConstructor;

    /** Are we visiting the class initializer method? */
    final boolean isClassInitializer;

    /** Are we the method readObject(java.io.ObjectInputStream)? */
    final boolean isReadObject;

    /** Are we the method readObjectNoData()? */
    final boolean isReadObjectNoData;

    /** Was the method originally synchronized? */
    final boolean wasSynchronized;

    /** Is the method static? */
    final boolean isStatic;

    /**
     * Must the class that contains the method implement the IIdObject
     * interface. If so, we need to update the constructors to initialize the
     * field flashlight$phantomObject.
     */
    private final boolean mustImplementIIdObject;

    /**
     * The happens-before records of the interesting happens-before "call in"
     * method that this method overrides, or <code>null</code> if the method
     * doen't override a happens before method whose callIn attribute is true.
     */
    private final CallInRecords callInRecords;

    /**
     * Index of the local variable that holds the happensBefore time stamp. Only
     * used if {@link #happensBeforeCallInRecord} is non-<code>null</code>.
     * Otherwise, the value is <code>-1</code>.
     * 
     * <p>
     * We need to store the time stamp in a variable because we might have to
     * get it at the start of the method. We don't need it until we handle a
     * return instruction. But we cannot guarantee that the extra junk didn't
     * get pushed on the stack between the time stamp and the method return
     * value. So it needs to be stored and retrieved when needed.
     */
    final int targetTimeStampVariableIndex;
    final int sourceTimeStampVariableIndex;
    
//    /**
//     * Index of the local variable that holds the reference to the connecting
//     * object in happens-before call-in collection methods. This variable is set
//     * at the beginning of the method by copying the actual that is passed to
//     * the method. We have to save it at the beginning of the method in case the
//     * local variable that it is passed into is reused. If we don't care about
//     * this then the value is <code>-1</code>.
//     */
//    private final int collectionConnectionVariableIndex;

    /**
     * Site index of the start of the method body for happens-before call-in
     * method. If we don't care about this, it is -1. Set my {@link #visitCode}.
     */
    long happensBeforeSiteIndex = -1L;

    /**
     * The current source line of code being rewritten. Driven by calls to
     * {@link #visitLineNumber}. This is {@code -1} when no line number
     * information is available.
     */
    int currentSrcLine = -1;

    /**
     * The global list of wrapper methods that need to be created. This list is
     * added to by this class, and is provided by the FlashlightClassRewriter
     * instance that create the method rewriter.
     */
    final Set<MethodCallWrapper> wrapperMethods;

    /**
     * Label for marking the start of the exception handler used when rewriting
     * class initializers, constructors and synchronized methods.
     */
    Label startOfExceptionHandler = null;

    /**
     * Label for marking the start of the exception handler used for wrapping
     * all constructors/methods with stack-trace building entry/exit calls.
     */
    Label startOfExecutionExceptionHandler = null;

    /**
     * The siteId used for marking the execution of this method. Passed to
     * {@link Store#methodExecution}.
     */
    long executionSiteId;

    /**
     * Label for marking the end of the current try-finally block when rewriting
     * synchronized methods.
     */
    Label endOfTryBlock = null;

    /**
     * If {@link #isConstructor} is <code>true</code>, this is initialized to a
     * state machine that fires after the super constructor call has been
     * detected; see {@link ObjectInitCallback}. This field is nulled once the
     * call has been detected.
     */
    ConstructorInitStateMachine stateMachine = null;

    /**
     * The class hierarchy and field model used to get unique field identifiers.
     */
    final ClassAndFieldModel classModel;

    /**
     * Refers to a thunk used to insert instructions after the following
     * instruction, if that instruction is a label. Otherwise, the operations
     * are inserted before the next instruction. See
     * {@link #delayForLabel(com.surelogic._flashlight.rewriter.FlashlightMethodRewriter.DelayedOutput)}
     * . Once the thunk has been executed, this is reset to null} .
     */
    DelayedOutput delayedForLabel = null;

    /**
     * The instruction node of the previous instruction if it was an ASTORE
     * instruction. Otherwise this is <code>null</code>.
     */
    VarInsnNode previousStoreInsn = null;

    /**
     * The instruction node of the previous instruction if it was an ALOAD
     * instruction. Otherwise this is <code>null</code>.
     */
    VarInsnNode previousLoadInsn = null;

    /**
     * When rewriting a synchronized method to use explicit locking, this holds
     * the id of the local variable that stores the lock object. Otherwise, it
     * is -1} .
     */
    int syncMethodLockVariable = -1;

    /**
     * Map from one label to another. The key label is a label we receive as a
     * start or end label in {@link #visitTryCatchBlock}. The value label is the
     * label we remap it to when we call {@code visitTryCatchBlock} to our
     * delegate method visitor. We do this because the compiled code can reuse
     * these labels for jump points, and we are sensitive to the start of try
     * blocks that follow monitorenter operations, and the end of try blocks
     * that immediately follow monitorexit operations. In particular, we insert
     * instrumentation code after the start and end of the try block. But we
     * don't want the program to later jump to the start or end of the try block
     * and reexecute our instrumentation code. First, this is wrong. But also,
     * the stack isn't set up properly for this to work and the bytecode
     * verifier rejects the code. So we insert a new label for the start/end of
     * the try block, and insert the original label after our instrumentation
     * code.
     */
    final Map<Label, Label> tryLabelMap = new HashMap<Label, Label>();

    /**
     * Factory for creating unique site identifiers.
     */
    final SiteIdFactory siteIdFactory;

    /**
     * The current site identifier
     */
    long siteId = 0;

    /**
     * The owner of the last "&lt;init&gt;" method called. Used by the object
     * init callback to determine if the flashlight$phantomObject field should
     * be initialized. If the last owner is the class being instrumented, then
     * the field is not initialized because we have a "this(...)" call, which
     * would have already initialized the field.
     */
    String lastInitOwner = null;

    /**
     * The index of the next new local variable to allocate.
     */
    int nextNewLocal = -1;

    /**
     * The set of methods that indirectly access shared state.
     */
    final IndirectAccessMethods accessMethods;

    /**
     * Factory method for generating a new instance. We need this so we can
     * manage the local variables sorter used by the instance.
     */
    public static MethodVisitor create(final InsnList instructions,
            final int access, final String mname, final String desc,
            final int numLocals, final MethodVisitor mv,
            final Configuration conf, final SiteIdFactory csif,
            final RewriteMessenger msg, final ClassAndFieldModel model,
            final HappensBeforeTable hbt, final IndirectAccessMethods am,
            final boolean inInt, final boolean mustImpl, final String fname,
            final String nameInternal, final String nameFullyQualified,
            final String superInternal, final Set<MethodCallWrapper> wrappers) {
        final FlashlightMethodRewriter methodRewriter = new FlashlightMethodRewriter(
                instructions, access, mname, desc, numLocals,
                new ExceptionHandlerReorderingMethodAdapter(mv), conf, csif,
                msg, model, hbt, am, inInt, mustImpl, fname, nameInternal,
                nameFullyQualified, superInternal, wrappers);
        return methodRewriter;
    }

    /**
     * Create a new method rewriter.
     * 
     * @param mname
     *            The simple name of the method being rewritten.
     * @param mv
     *            The {@code MethodVisitor} to delegate to.
     * @param fname
     *            The name of the source file that contains the class being
     *            rewritten.
     * @param nameInternal
     *            The internal name of the class being rewritten.
     * @param nameFullyQualified
     *            The fully qualified name of the class being rewritten.
     * @param wrappers
     *            The set of wrapper methods that this visitor should add to.
     */
    @SuppressWarnings("synthetic-access")
    private FlashlightMethodRewriter(final InsnList instList, final int access,
            final String mname, final String desc, final int numLocals,
            final ExceptionHandlerReorderingMethodAdapter mv,
            final Configuration conf, final SiteIdFactory csif,
            final RewriteMessenger msg, final ClassAndFieldModel model,
            final HappensBeforeTable hbt, final IndirectAccessMethods am,
            final boolean inInt, final boolean mustImpl,
            final String sourceFileName, final String nameInternal,
            final String classBeingAnalyzedFullyQualified,
            final String superInternal, final Set<MethodCallWrapper> wrappers) {
        super(Opcodes.ASM5, mv);
        this.mv = mv; // Initialize the shadow casting reference to the method
                      // visitor: must alias super.mv
        currentInsn = instList.getFirst();
        config = conf;
        siteIdFactory = csif;
        messenger = msg;
        classModel = model;
        happensBefore = hbt;
        accessMethods = am;
        inInterface = inInt;
        mustImplementIIdObject = mustImpl;
        wasSynchronized = (access & Opcodes.ACC_SYNCHRONIZED) != 0;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        methodName = mname;
        methodDesc = desc;
        isConstructor = mname.equals(INITIALIZER);
        isClassInitializer = mname.equals(CLASS_INITIALIZER);
        isReadObject = mname.equals(FlashlightNames.READ_OBJECT.getName())
                && desc.equals(FlashlightNames.READ_OBJECT.getDescriptor());
        isReadObjectNoData = mname.equals(FlashlightNames.READ_OBJECT_NO_DATA
                .getName())
                && desc.equals(FlashlightNames.READ_OBJECT_NO_DATA
                        .getDescriptor());
        classBeingAnalyzedInternal = nameInternal;
        packageNameInternal = ClassAndFieldModel.getPackage(nameInternal);
        superClassInternal = superInternal;
        wrapperMethods = wrappers;
        nextNewLocal = numLocals;

        if (isConstructor) {
            stateMachine = new ConstructorInitStateMachine(
                    new ObjectInitCallback());
        } else {
            stateMachine = null;
        }

        /* Reset the site factory for a new method */
        siteIdFactory.setMethodLocation(sourceFileName,
                classBeingAnalyzedFullyQualified, inInterface, methodName,
                desc, access);

        CallInRecords records;
        try {
          final Set<HappensBeforeRule> cir =
              hbt.isInsideHappensBefore(nameInternal, mname, desc);
          records = cir.isEmpty() ? null : new CallInRecords(this, cir);
        } catch (final ClassNotFoundException e) {
          records = null;
          messenger.warning(
              "Provided classpath is incomplete 100: couldn't find class "
                  + e.getMissingClass());
        }
        callInRecords = records;
        targetTimeStampVariableIndex =
            records == null ? -1 : newLocal(Type.LONG_TYPE);
        sourceTimeStampVariableIndex =
            records == null ? -1 : newLocal(Type.LONG_TYPE);
//        collectionConnectionVariableIndex =
//            records == null ? -1 : newLocal(Type.getObjectType("java/lang/Object"));
    }

    @Override
    public void visitCode() {
        /*
         * We are just about to start visiting instructions, so we cannot have
         * any delayed instructions yet.
         */
        mv.visitCode();

        if (isConstructor) {
            /*
             * Constructors don't call methodExecution(); they already call
             * constructorExecution().
             */
            updateSiteIdentifier();
        } else {
            /*
             * Used to initialize the site identifier in case the class doesn't
             * have line number information, but this now happens in
             * insertMethodExecutionPrefix() when a site is generated for stack
             * trace info.
             */
            insertMethodExecutionPrefix();
        }

        // Initialize the flashlight$phantomClass field
        if (isClassInitializer) {
            insertClassInitializerCode();
            insertClassInitPrefix();
        }
        if (wasSynchronized && config.rewriteSynchronizedMethod) {
            insertSynchronizedMethodPrefix();
        }

        /*
         * If the method is readObject(ObjectInputStream) or readObjectNoData(),
         * we need to insert code to initialize the flashlight$phantomObject
         * field.
         */
        if (isReadObject || isReadObjectNoData) {
            ByteCodeUtils.initializePhantomObject(mv, config,
                    classBeingAnalyzedInternal);
        }

        /*
         * If we are inside a method that overrides a happens-before 'callIn'
         * 'target' method, we need to get the time stamp and store it in a
         * local variable.
         */
        if (callInRecords != null) {
          happensBeforeSiteIndex = siteIdFactory.getSiteId(currentSrcLine);
          if (callInRecords.hasTarget()) {
              pushAndStoreTimeStamp(targetTimeStampVariableIndex);
          }
          for (final CallInRecord record : callInRecords) {
            final Integer whichActualBoxed = record.getArgument(); 
            if (whichActualBoxed != null) {
              final int whichActual = whichActualBoxed;
              /*
               * N.B. if -1, then the return value is used; if 0 then the 
               * receiver is used. Otherwise we now
               * have to find the local variable that holds the actual
               * parameter so we can copy it for later use.
               */
              if (whichActual > 0) {
                  // START WORKING HERE

                  final Type[] argTypes = Type.getArgumentTypes(methodDesc);
                  /*
                   * We know the method is not-static so the first actual
                   * argument is in variable 1
                   */
                  int copyFromVar = 1;
                  for (int current = 1; current < whichActual; current++) {
                      copyFromVar += argTypes[current - 1].getSize();
                  }

                  // Load the actual argument, and then store it back in our
                  // variable
                  // ...
                  mv.visitVarInsn(Opcodes.ALOAD, copyFromVar);
                  // ..., object ref from the <whichActual>th formal parameter
                  mv.visitVarInsn(Opcodes.ASTORE, record.getLocal());
                  // ..
              }
            }
          }
        }
        
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        /*
         * This callback does not correspond to an instruction, so don't worry
         * about delayed instructions. May affect the line number of inserted
         * instructions.
         */
        mv.visitLineNumber(line, start);
        currentSrcLine = line;
        updateSiteIdentifier();

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        doNotInstrument();
    }

    @Override
    public void visitInsn(final int opcode) {
        if (stateMachine != null) {
            stateMachine.visitInsn(opcode);
        }

        if (opcode == Opcodes.MONITORENTER && config.rewriteMonitorenter) {
            handlePreviousAload();
            // previous store is dealt with in rewriteMonitorenter
            insertDelayedCode();
            // Frame modeling is dealt with in rewriteMonitorenter
            rewriteMonitorenter();
        } else if (opcode == Opcodes.MONITOREXIT && config.rewriteMonitorexit) {
            // previous load is dealt with in rewriteMonitorexit()
            handlePreviousAstore();
            insertDelayedCode();
            // Frame modeling is dealt with in rewriteMonitorenter
            rewriteMonitorexit();
        } else if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            handlePreviousInstructions();

            /*
             * Deal with happens-before "callIn" methods. Should be done before
             * we signal any kind of end of method execution to the store.
             */
            if (callInRecords != null) {
              /*
               * If the method overrides a "source" method then we have to get the
               * time stamp to use below. If the method overrides a "target" method
               * then the time stamp was retrieved at the start of the methods
               * execution.
               */
              if (callInRecords.hasSource()) {
                  pushAndStoreTimeStamp(sourceTimeStampVariableIndex);
              }

              for (final CallInRecord record : callInRecords) {
                insertHappensBeforeCallIn(record, opcode);
              }
            }

            if (wasSynchronized && config.rewriteSynchronizedMethod) {
                insertSynchronizedMethodExit();
            }
            if (isConstructor && config.rewriteConstructorExecution) {
                insertConstructorExecution(false);
            }
            if (isClassInitializer) {
                insertClassInit(false);
            }
            if (!isConstructor) {
                insertMethodExecution(false);
            }
            currentInsn.accept(mv);

            if (wasSynchronized && config.rewriteSynchronizedMethod) {
                /* Start a new try-block */
                final Label startOfTryBlock = new Label();
                endOfTryBlock = new Label();
                mv.appendTryCatchBlock(startOfTryBlock, endOfTryBlock,
                        startOfExceptionHandler, null);
                mv.visitLabel(startOfTryBlock);
            }
        } else if (opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD) {
            handlePreviousInstructions();
            rewriteArrayLoad(opcode == Opcodes.LALOAD
                    || opcode == Opcodes.DALOAD);
        } else if (opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE) {
            handlePreviousInstructions();
            rewriteArrayStore(opcode == Opcodes.LASTORE
                    || opcode == Opcodes.DASTORE);
        } else {
            handlePreviousInstructions();
            currentInsn.accept(mv);
        }

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        handlePreviousInstructions();

        if (opcode == Opcodes.PUTFIELD && config.rewritePutfield) {
            rewritePutfield(owner, name, desc);
        } else if (opcode == Opcodes.PUTSTATIC && config.rewritePutstatic) {
            rewritePutstatic(owner, name, desc);
        } else if (opcode == Opcodes.GETFIELD && config.rewriteGetfield) {
            rewriteGetfield(owner, name, desc);
        } else if (opcode == Opcodes.GETSTATIC && config.rewriteGetstatic) {
            rewriteGetstatic(owner, name, desc);
        } else {
            // Shouldn't be any other case
            throw new IllegalStateException(
                    "Field instruction is not PUTFEILD, PUTSTATIC, GETFIELD, or GETSTATIC");
        }

        if (stateMachine != null) {
            stateMachine.visitFieldInsn(opcode, owner, name, desc);
        }

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        handlePreviousInstructions();

        /*
         * The owner might be an array class, such as "[[La/b/C;" or
         * "[Ljava/lang.Object;". The only current situation where this can
         * occur is when the the method being called is "clone()". In this case
         * we have to be careful because we don't have array classes modeled in
         * the class model. We have to avoid doing things that would cause us to
         * look up the non-existent class object.
         */
        final boolean isArray = owner.charAt(0) == '[';
        if (!isArray) {
            /*
             * SPECIAL CASE: If the method being called in Runtime.halt(int), we
             * need to insert a call to Store.shutdown(). No point in
             * instrumenting the halt call because it will occur after the Store
             * and everything else has been stopped.
             */
            try {
                if (callsMethod(FlashlightNames.JAVA_LANG_RUNTIME,
                        FlashlightNames.HALT, owner, name, desc)) {
                    // Insert call to Store.shutdown()
                    ByteCodeUtils.callStoreMethod(mv, config,
                            FlashlightNames.SHUTDOWN);

                    // Insert original call to halt(int)
                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }
            } catch (final ClassNotFoundException e) {
                messenger
                        .warning("Provided classpath is incomplete 2: couldn't find class "
                                + e.getMissingClass());
            }
        }

        // 2014-06-06: Now we do because we need for lambda expressions
        // /* We don't instrument calls from within synthetic methods */
        // if (isSynthetic) {
        // /*
        // * Still track the last init call.
        // */
        // if (name.equals(FlashlightNames.CONSTRUCTOR)) {
        // lastInitOwner = owner;
        // }
        // mv.visitMethodInsn(opcode, owner, name, desc, itf);
        // } else {
        /*
         * Check if we are calling a method makes indirect use of aggregated
         * state. Methods on an array class NEVER do. So we avoid potential
         * lookup problems by skipping them.
         */
        IndirectAccessMethod indirectAccess = null;
        if (!isArray && config.instrumentIndirectAccess) { // we might have
                                                           // indirect access
            try {
                indirectAccess = accessMethods.get(owner, name, desc);
            } catch (final ClassNotFoundException e) {
                // Couldn't find the class. Used to insert an exception here
                // throwMissingClass(e.getMissingClass());
                // But now we soldier on with a warning message, and figure that
                // the
                // call is not an indirect access call
                indirectAccess = null;
                messenger.warning("In Method " + methodName
                        + ": Couldn't find class " + e.getMissingClass() + ".");
            }
        }

        if (opcode == Opcodes.INVOKEVIRTUAL) {
            if (config.rewriteInvokevirtual) {
                rewriteMethodCall(Opcodes.INVOKEVIRTUAL, indirectAccess, owner,
                        name, desc);
            } else {
                currentInsn.accept(mv);
            }
        } else if (opcode == Opcodes.INVOKESPECIAL) {
            boolean outputOriginalCall = true;
            if (config.rewriteInvokespecial) {
                if (!name.equals(FlashlightNames.CONSTRUCTOR)) {
                    outputOriginalCall = false;
                    rewriteMethodCall(Opcodes.INVOKESPECIAL, indirectAccess,
                            owner, name, desc);
                } else {
                    lastInitOwner = owner;
                    if (config.rewriteInit) {
                        outputOriginalCall = false;
                        rewriteConstructorCall(indirectAccess, owner, name,
                                desc);
                    } else {
                        // output the original instruction
                    }
                }
            } else {
                if (name.equals(FlashlightNames.CONSTRUCTOR)) {
                    lastInitOwner = owner;
                }
            }
            if (outputOriginalCall) {
                currentInsn.accept(mv);
            }
        } else if (opcode == Opcodes.INVOKEINTERFACE) {
            if (config.rewriteInvokeinterface) {
                rewriteMethodCall(Opcodes.INVOKEINTERFACE, indirectAccess,
                        owner, name, desc);
            } else {
                currentInsn.accept(mv);
            }
        } else if (opcode == Opcodes.INVOKESTATIC) {
            if (config.rewriteInvokestatic) {
                rewriteMethodCall(Opcodes.INVOKESTATIC, indirectAccess, owner,
                        name, desc);
            } else {
                currentInsn.accept(mv);
            }
        } else { // Unknown, but safe
            currentInsn.accept(mv);
        }
        // }

        if (stateMachine != null) {
            stateMachine.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        /*
         * If the called method is ObjectInputStream.defaultReadObject() or
         * ObjectInputStream.readFields(), we need to generate field write
         * events for each serializable field in this class. This can never be
         * the case if the method owner is an array class.
         */
        if (!isArray) {
            try {
                if (callsMethod(FlashlightNames.JAVA_IO_OBJECTINPUTSTREAM,
                        FlashlightNames.DEFAULT_READ_OBJECT, owner, name, desc)
                        || callsMethod(
                                FlashlightNames.JAVA_IO_OBJECTINPUTSTREAM,
                                FlashlightNames.READ_FIELDS, owner, name, desc)) {
                    insertFieldWrites();
                }
            } catch (final ClassNotFoundException e) {
                messenger
                        .warning("Provided classpath is incomplete 3: couldn't find class "
                                + e.getMissingClass());
            }
        }

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        handlePreviousInstructions();
        if (wasSynchronized && config.rewriteSynchronizedMethod) {
            insertSynchronizedMethodPostfix();
        }

        if (isClassInitializer) {
            insertClassInitPostfix();
        }

        if (isConstructor && config.rewriteConstructorExecution) {
            insertConstructorExecutionPostfix();
        }

        if (!isConstructor) {
            insertMethodExecutionPostfix();
        }

        /*
         * We require the use of a ClassWRiter with the COMPUTE_MAXES or
         * COMPUTE_FRAMES flag set. So we just pass in the original values here
         * and let ASM figure out the appropriate values.
         */
        mv.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        doNotInstrument();
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc,
            final Handle bsm, final Object... bsmArgs) {
        handlePreviousInstructions();

        if (stateMachine != null) {
            stateMachine.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        currentInsn.accept(mv);

        /*
         * See if the bootstrap method indicates that this call is being used to
         * generate a closure object reference.
         */
        if (bsm.getTag() == Opcodes.H_INVOKESTATIC
                && bsm.getOwner().equals(FlashlightNames.LAMBDA_METAFACTORY)
                && bsm.getName().equals(
                        FlashlightNames.LAMBDA_METAFACTORY_METAFACTORY
                                .getName())
                && bsm.getDesc().equals(
                        FlashlightNames.LAMBDA_METAFACTORY_METAFACTORY
                                .getDescriptor())) {
            final Type fiType = Type.getMethodType(desc).getReturnType();
            final String methodDesc = ((Type) bsmArgs[0]).getDescriptor();
            final Handle wrappedHandle = (Handle) bsmArgs[1];

            /*
             * Top of stack is the closure object: ..., closure
             */
            mv.visitInsn(Opcodes.DUP);
            // ..., closure, closure
            mv.visitLdcInsn(fiType.getInternalName());
            // ..., closure, closure, functional interface internal name
            mv.visitLdcInsn(name);
            // ..., closure, closure, functional interface internal name, method
            // name
            mv.visitLdcInsn(methodDesc);
            // ..., closure, closure, functional interface internal name, method
            // name, method descriptor
            ByteCodeUtils.pushIntegerConstant(mv, wrappedHandle.getTag());
            // ..., closure, closure, functional interface internal name, method
            // name, method descriptor, behavior
            mv.visitLdcInsn(wrappedHandle.getOwner());
            // ..., closure, closure, functional interface internal name, method
            // name, method descriptor, behavior, wrapped owner
            mv.visitLdcInsn(wrappedHandle.getName());
            // ..., closure, closure, functional interface internal name, method
            // name, method descriptor, behavior, wrapped owner, wrapped name
            mv.visitLdcInsn(wrappedHandle.getDesc());
            // ..., closure, closure, functional interface internal name, method
            // name, method descriptor, behavior, wrapped owner, wrapped name,
            // wrapped descriptor

            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.CLOSURE_CREATION);
            // ..., closure
        }

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        doNotInstrument();
    }

    @Override
    public void visitLabel(final Label label) {
        handlePreviousAload();
        handlePreviousAstore();
        if (stateMachine != null) {
            stateMachine.visitLabel(label);
        }

        /*
         * If this label is the start of a try-block, output our new start label
         * instead.
         * 
         * We output the original label below after handling any delayed output.
         * That is, the original label follows any instrumentation we might
         * insert.
         */
        final Label newLabel = tryLabelMap.get(label);
        mv.visitLabel(newLabel != null ? newLabel : label);
        insertDelayedCode();
        if (newLabel != null) {
            mv.visitLabel(label);
        }

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        doNotInstrument();
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
            final Label[] labels) {
        doNotInstrument();
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        doNotInstrument();
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final Label dflt, final Label... labels) {
        doNotInstrument();
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        handlePreviousInstructions();
        if (stateMachine != null) {
            stateMachine.visitVarInsn(opcode, var);
        }
        if (opcode == Opcodes.ASTORE) {
            previousStoreInsn = (VarInsnNode) currentInsn;
        } else if (opcode == Opcodes.ALOAD) {
            previousLoadInsn = (VarInsnNode) currentInsn;
        } else {
            currentInsn.accept(mv);
        }

        currentInsn = currentInsn.getNext();
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        /*
         * Called before visitCode(), so we don't have worry about inserting any
         * delayed instructions.
         */
        return mv.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
        /*
         * Called before visitCode(), so we don't have worry about inserting any
         * delayed instructions.
         */
        return mv.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter,
            final String desc, final boolean visible) {
        /*
         * Called before visitCode(), so we don't have worry about inserting any
         * delayed instructions.
         */
        return mv.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(final int typeRef,
            final TypePath typePath, final String desc, final boolean visible) {
        /*
         * Always ignore this because it's too late now to do anything with the
         * information. Instead we output instruction-level annotations when we
         * output the original instruction based on the instruction node from
         * the tree model.
         */
        return null;
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(final int typeRef,
            final TypePath typePath, final Label[] start, final Label[] end,
            final int[] index, final String desc, final boolean visible) {
        return mv.visitLocalVariableAnnotation(typeRef, typePath, start, end,
                index, desc, visible);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
            final TypePath typePath, final String desc, final boolean visible) {
        return mv.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef,
            final TypePath typePath, final String desc, final boolean visible) {
        /*
         * Called before visitCode(), so we don't have worry about inserting any
         * delayed instructions.
         */
        return mv.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public void visitAttribute(final Attribute attr) {
        /*
         * Called before visitCode(), so we don't have worry about inserting any
         * delayed instructions.
         */
        mv.visitAttribute(attr);
    }

    @Override
    public void visitParameter(final String name, final int access) {
        /*
         * Called before visitCode(), so we don't have worry about inserting any
         * delayed instructions.
         */
        mv.visitParameter(name, access);
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
        /*
         * This callback doesn't represent a bytecode instruction, so we don't
         * do anything about delayed instructions yet. We have ASM regenerate
         * the frame information any how, so we can just drop this.
         */
        mv.visitFrame(type, nLocal, local, nStack, stack);

        currentInsn = currentInsn.getNext();
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        doNotInstrument();
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end,
            final Label handler, final String type) {
        /*
         * This callback doesn't represent a bytecode instruction, so we don't
         * do anything about delayed instructions yet.
         */

        /*
         * Remap the label for the start of the try block. But only if we
         * haven't seen it before.
         */
        Label newStart = tryLabelMap.get(start);
        if (newStart == null) {
            newStart = new Label();
            tryLabelMap.put(start, newStart);
        }
        Label newEnd = tryLabelMap.get(end);
        if (newEnd == null) {
            newEnd = new Label();
            tryLabelMap.put(end, newEnd);
        }

        mv.visitTryCatchBlock(newStart, newEnd, handler, type);
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
        /*
         * This callback doesn't represent a bytecode instruction, so we don't
         * do anything about delayed instructions yet.
         */
        mv.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitEnd() {
        /*
         * Sanity check: instruction count should equal the size of the
         * instruction list.
         */
        if (currentInsn != null) {
            throw new IllegalStateException("Bad instruction count");
        }

        /* Output the site identifiers */
        try {
            siteIdFactory.closeMethod(classModel);
        } catch (final ClassNotFoundException e) {
            messenger
                    .warning("Provided classpath is incomplete 4: couldn't find class "
                            + e.getMissingClass());
        }

        try {
            /* visitMaxs already cleared out the remaining delayed instructions. */
            mv.visitEnd();
        } catch (final MissingClassException e) {
            // Add missing information and rethrow
            e.completeException(classBeingAnalyzedInternal, methodName,
                    methodDesc);
            throw e;
        }
    }

    // =========================================================================
    // == Utility methods
    // =========================================================================

    /**
     * Return the index of a new local variable.
     */
    @Override
    public int newLocal(final Type type) {
        final int local = nextNewLocal;
        nextNewLocal += type.getSize();
        return local;
    }

    /**
     * Abstract class for holding instrumentation code that needs executed after
     * the next instruction, if the next instruction is a label, or before the
     * next instruction if it is not a label.
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
     * <li>Called after we have visited a label and called {@code visitLabel} on
     * the method visitor delegate.
     * <li>Called when we are starting to visit a non-label instruction, before
     * we have called {@code visit...} on the method visitor delegate.
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
     * was an aload. If so, we want to make sure we don't insert any
     * instructions between the ALOAD and the MONITOREXIT, or the JIT compiler
     * will not compile the method to native code. So when we encounter an ALOAD
     * in {@link #visitVarInsn} we record the variable id, but do not forward
     * the ALOAD to the method visitor delegate. When we visit an operation
     * other than monitorexit, we call this method first to make sure we forward
     * the ALOAD to the method visitor delegate before we do anything else. When
     * we visit a monitorexit operation we handle this case specially.
     */
    private void handlePreviousAload() {
        if (previousLoadInsn != null) {
            previousLoadInsn.accept(mv);
            previousLoadInsn = null;
        }
    }

    /**
     * For monitorenter operations we want to know if the previous instruction
     * was an astore. If so, we want to make sure we don't insert any
     * instructions between the astore and the monitorenter, or the JIT compiler
     * will not compile the method to native code. So when we encounter an
     * astore in {@link #visitVarInsn} we record the variable id, but do not
     * forward the astore to the method visitor delegate. When we visit an
     * operation other than monitorenter, we call this method first to make sure
     * we forward the astore to the method visitor delegate before we do
     * anything else. When we visit a monitorenter operation we handle this case
     * specially.
     */
    private void handlePreviousAstore() {
        if (previousStoreInsn != null) {
            previousStoreInsn.accept(mv);
            previousStoreInsn = null;
        }
    }

    private void handlePreviousInstructions() {
        handlePreviousAload();
        handlePreviousAstore();
        insertDelayedCode();
    }

    // =========================================================================
    // == Insert Bookkeeping code
    // =========================================================================

    /**
     * Insert code into the class initializer that inits any flashlight-specific
     * state that must be set before any calls to the Store can be made from
     * this class.
     */
    private void insertClassInitializerCode() {
        // Stack is empty (we are at the beginning of the method!)

        /*
         * Set flashlight$withinClass by calling Store.getClassPhantom()
         */
        ByteCodeUtils.pushClass(mv, classBeingAnalyzedInternal);
        // Class
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.GET_CLASS_PHANTOM);
        // ClassPhantomReference
        mv.visitFieldInsn(Opcodes.PUTSTATIC, classBeingAnalyzedInternal,
                FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
                FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC);
        // empty stack

        // resume
    }

    // =========================================================================
    // == Handle instructions that we don't instrument
    // =========================================================================

    private void doNotInstrument() {
        handlePreviousInstructions();

        if (stateMachine != null) {
            currentInsn.accept(stateMachine);
        }
        // Output the original instruction and any annotations associated with
        // it
        currentInsn.accept(mv);

        currentInsn = currentInsn.getNext();
    }

    // =========================================================================
    // == Rewrite new/<init>
    // =========================================================================

    private void insertClassInitPrefix() {
        /* Create event */
        insertClassInit(true);

        /* Set up finally handler */
        final Label startOfInitializer = new Label();
        startOfExceptionHandler = new Label();
        mv.appendTryCatchBlock(startOfInitializer, startOfExceptionHandler,
                startOfExceptionHandler, null);
        // mv.visitTryCatchBlock(startOfInitializer,
        // startOfExceptionHandler, startOfExceptionHandler, null);

        /* Start of initializer */
        mv.visitLabel(startOfInitializer);
    }

    private void insertClassInitPostfix() {
        // exception
        mv.visitLabel(startOfExceptionHandler);
        insertClassInit(false);
        // exception

        /* Rethrow the exception */
        mv.visitInsn(Opcodes.ATHROW);
        startOfExceptionHandler = null;
    }

    private void insertClassInit(final boolean before) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, before);
        // ..., before
        ByteCodeUtils.pushClass(mv, classBeingAnalyzedInternal);
        // ..., before, clazz
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.CLASS_INIT);
        // ...
    }

    private void insertMethodExecutionPrefix() {
        /* Create event */
        executionSiteId = siteIdFactory.getSiteId(currentSrcLine); // ,
                                                                   // methodName,
                                                                   // classBeingAnalyzedInternal,
                                                                   // methodDesc);
        // init the site identifier for the whole method
        siteId = executionSiteId;
        insertMethodExecution(true);

        /* Set up finally handler */
        final Label startOfOriginalMethod = new Label();
        startOfExecutionExceptionHandler = new Label();
        mv.appendTryCatchBlock(startOfOriginalMethod,
                startOfExecutionExceptionHandler,
                startOfExecutionExceptionHandler, null);

        /* Start of constructor */
        mv.visitLabel(startOfOriginalMethod);
    }

    private void insertMethodExecutionPostfix() {
        mv.visitLabel(startOfExecutionExceptionHandler);

        // exception
        insertMethodExecution(false);
        // exception

        /* Rethrow the exception */
        mv.visitInsn(Opcodes.ATHROW);

        startOfExecutionExceptionHandler = null;
    }

    private void insertMethodExecution(final boolean before) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, before);
        // ..., before
        pushSiteIdentifier(executionSiteId);
        // ..., before, siteId
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.METHOD_EXECUTION);
        // ...
    }

    private void insertConstructorExecutionPrefix() {
        /* Create event */
        insertConstructorExecution(true);

        /* Set up finally handler */
        final Label startOfOriginalConstructor = new Label();
        startOfExceptionHandler = new Label();
        mv.appendTryCatchBlock(startOfOriginalConstructor,
                startOfExceptionHandler, startOfExceptionHandler, null);

        /* Start of constructor */
        mv.visitLabel(startOfOriginalConstructor);
    }

    private void insertConstructorExecutionPostfix() {
        mv.visitLabel(startOfExceptionHandler);

        // exception (+1)
        insertConstructorExecution(false); // +4 on stack (thus needs +5 because
                                           // of exception already on stack)
        // exception

        /* Rethrow the exception */
        mv.visitInsn(Opcodes.ATHROW);

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
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.CONSTRUCTOR_EXECUTION);
        // ...
    }

    private void insertHappensBeforeCallIn(
        final CallInRecord record, final int returnOpcode) {
        /*
         * N.B. This method is very similar to the method
         * MethodCall.instrumentHappensBefore(). Should really find a way to
         * share code between the two to prevent problems in the future.
         */

        /*
         * stack is ..., [return value]
         * 
         * The return value may be absent (RETURN). It can be a category 1 or
         * category 2 type.
         */

        /*
         * At this point we know that the time stamp is stored in a local
         * variable
         */

        /*
         * Check the return value of the method call to see if we should
         * generate an event.
         */
        final Label skip = new Label();
        final ReturnCheck check = record.getReturnCheck();
        if (check != ReturnCheck.NONE) {
            final int opCode;
            switch (check) {
            case NOT_NULL:
                opCode = Opcodes.IFNULL;
                break;
            case NULL:
                opCode = Opcodes.IFNONNULL;
                break;
            case TRUE:
                opCode = Opcodes.IFEQ;
                break;
            case FALSE:
                opCode = Opcodes.IFNE;
                break;
            default:
                opCode = Opcodes.NOP;
            }

            /*
             * We know there is a return value, because otherwise we wouldn't be
             * trying to check it here. We also know the return value has to be
             * category 1.
             */
            // ..., return value
            mv.visitInsn(Opcodes.DUP);
            // ..., return value, return value
            mv.visitJumpInsn(opCode, skip);
            // ..., return value
        }

        // ..., [return value]
        record.invokeSwitch(
            new HappensBeforeCallInSwitch(
                record.getLocal(),
                record.isSource() ? sourceTimeStampVariableIndex : targetTimeStampVariableIndex));
        // ..., [return value]

        mv.visitLabel(skip);
        // ..., [return value]
    }

    private final class HappensBeforeCallInSwitch
    implements HappensBeforeSwitch {
      final int argumentLocal;
      final int timeStampVariable;
      
      public HappensBeforeCallInSwitch(final int local, final int tsVar) {
        argumentLocal = local;
        timeStampVariable = tsVar;
      }
      
        @Override
        public void caseHappensBefore(final HappensBeforeRule hb) {
            // ..., [return value]
            mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
            // ..., [return value], nanoTime (long)
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // ..., [return value], nanoTime (long), threadRef
            mv.visitLdcInsn(hb.getId());
            // ... [return value], nanoTime (long), threadRef, id
            ByteCodeUtils.pushLongConstant(mv, happensBeforeSiteIndex);
            // ..., [return value], nanoTime (long), threadRef, id, callSiteId
            // (long)
            mv.visitInsn(Opcodes.ACONST_NULL);
            // ..., [return value], nanoTime (long), threadRef, id, callSideId
            // (long), null
            ByteCodeUtils.pushBooleanConstant(mv, true); // Call-in method
                                                         // situation
            // ..., [return value], nanoTime (long), threadRef, id, callSideId
            // (long), null, true
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.HAPPENS_BEFORE_THREAD);
            // ..., [return value]
        }

        @Override
        public void caseHappensBeforeObject(final HappensBeforeObjectRule hb) {
            // ..., [return value]
            mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
            // ..., [return value], nanoTime (long)
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // ..., [return value], nanoTime (long), object
            mv.visitLdcInsn(hb.getId());
            // ..., [return value], nanoTime (long), object, id
            ByteCodeUtils.pushLongConstant(mv, happensBeforeSiteIndex);
            // ..., [return value], nanoTime (long), object, id, callSiteId
            // (long)
            mv.visitInsn(Opcodes.ACONST_NULL);
            // ..., [return value], nanoTime (long), object, id, callSiteId
            // (long), null
            ByteCodeUtils.pushBooleanConstant(mv, true); // Call-in method
                                                         // situation
            // ..., [return value], nanoTime (long), object, id, callSiteId
            // (long), null, true
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.HAPPENS_BEFORE_OBJECT);
            // ..., [return value]
        }

        @Override
        public void caseHappensBeforeCollection(
                final HappensBeforeCollectionRule hb) {
            // ..., [return value]

            /*
             * Check if the arg pos is -1. If so, then we use the return value
             * as the connecting object. In this case the return value must be
             * an object reference, so we don't have to worry about the size of
             * the return value being 2.
             */
            if (hb.isParamReturnValue()) {
                mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
                // ..., return value, nanoTime (long)
                mv.visitInsn(Opcodes.DUP2_X1);
                // ..., nanonTime (long), return value, nanoTime (long)
                mv.visitInsn(Opcodes.POP2);
                // ..., nanonTime (long), return value
                mv.visitInsn(Opcodes.DUP_X2);
                // ..., return value, nanonTime (long), return value
            } else {
                mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
                // ..., [return value], nanoTime (long)

                // push the parameter we stored at the beginning of the method
                mv.visitVarInsn(Opcodes.ALOAD, argumentLocal);
                // ..., [return value], nanoTime (long), item
            }
            // ..., [return value], nanoTime (long), item

            // collection is the receiver
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            // ..., [return value], nanoTime (long), item, collection
            mv.visitLdcInsn(hb.getId());
            // ..., [return value], nanoTime (long), item, collection, id
            ByteCodeUtils.pushLongConstant(mv, happensBeforeSiteIndex);
            // ..., [return value], nanoTime (long), item, collection, id,
            // callSiteId (long)
            mv.visitInsn(Opcodes.ACONST_NULL);
            // ..., [return value], nanoTime (long), item, collection, id,
            // callSiteId (long), null
            ByteCodeUtils.pushBooleanConstant(mv, true); // Call-in method
                                                         // situation
            // ..., [return value], nanoTime (long), item, collection, id,
            // callSiteId (long), null, true
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.HAPPENS_BEFORE_COLLECTION);
            // ..., [return value]
        }

        @Override
        public void caseHappensBeforeExecutor(final HappensBeforeExecutorRule hb) {
            // ..., [return value]

            /*
             * Check if the arg pos is -1. If so, then we use the return value
             * as the connecting object. In this case the return value must be
             * an object reference, so we don't have to worry about the size of
             * the return value being 2.
             */
            if (hb.isParamReturnValue()) {
                mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
                // ..., return value, nanoTime (long)
                mv.visitInsn(Opcodes.DUP2_X1);
                // ..., nanonTime (long), return value, nanoTime (long)
                mv.visitInsn(Opcodes.POP2);
                // ..., nanonTime (long), return value
                mv.visitInsn(Opcodes.DUP_X2);
                // ..., return value, nanonTime (long), return value
            } else if (hb.isParamReceiver()) { // do we want the receiver?
                mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
                // ..., [return value], nanoTime (long)

                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // ..., [return value], nanoTime (long), receiver
            } else { // regular parameter
                mv.visitVarInsn(Opcodes.LLOAD, timeStampVariable);
                // ..., [return value], nanoTime (long)

                // push the parameter we stored at the beginning of the method
                mv.visitVarInsn(Opcodes.ALOAD, argumentLocal);
                // ..., [return value], nanoTime (long), object
            }
            // ..., [return value], nanoTime (long), object

            // ..., [return value], nanoTime (long), object
            mv.visitLdcInsn(hb.getId());
            // ..., [return value], nanoTime (long), object, id
            ByteCodeUtils.pushLongConstant(mv, happensBeforeSiteIndex);
            // ..., [return value], nanoTime (long), object, id, callSiteId
            // (long)
            mv.visitInsn(Opcodes.ACONST_NULL);
            // ..., [return value], nanoTime (long), object, id, callSiteId
            // (long), null
            ByteCodeUtils.pushBooleanConstant(mv, true); // Call-in method
                                                         // situation
            // ..., [return value], nanoTime (long), object, id, callSiteId
            // (long), null, true
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.HAPPENS_BEFORE_EXECUTOR);
            // ..., [return value]
        }
    }

    private void rewriteConstructorCall(
            final IndirectAccessMethod indirectAccess, final String owner,
            final String name, final String desc) {
        final MethodInsnNode methodInsn = (MethodInsnNode) currentInsn;

        final long siteId = siteIdFactory.getSiteId(currentSrcLine, name,
                owner, desc);
        if (isConstructor && stateMachine != null) {
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc, false);
        } else {
            if (indirectAccess != null) {
                final IndirectAccessMethodInstrumentation method = new InstanceIndirectAccessMethodInstrumentation(
                        messenger, classModel, happensBefore, siteId,
                        methodInsn, indirectAccess, this);
                method.popReceiverAndArguments(mv);
                method.recordIndirectAccesses(mv, config);
                method.pushReceiverAndArguments(mv);
            }

            // ...
            ByteCodeUtils.pushBooleanConstant(mv, true);
            // ..., true
            pushSiteIdentifier(siteId);
            // ..., true, siteId
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.CONSTRUCTOR_CALL);

            final Label start = new Label();
            final Label end = new Label();
            final Label handler = new Label();
            final Label resume = new Label();
            mv.prependTryCatchBlock(start, end, handler, null);

            /* Original call */
            mv.visitLabel(start);
            methodInsn.accept(mv);
            mv.visitLabel(end);

            /* Normal return */
            // ...
            ByteCodeUtils.pushBooleanConstant(mv, false);
            // ..., false
            pushSiteIdentifier(siteId);
            // ..., false, siteId
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.CONSTRUCTOR_CALL);
            // ...
            mv.visitJumpInsn(Opcodes.GOTO, resume);

            /* exception handler */
            mv.visitLabel(handler);
            // ex
            ByteCodeUtils.pushBooleanConstant(mv, false);
            // ex, false
            pushSiteIdentifier(siteId);
            // ex, false, siteId
            ByteCodeUtils.callStoreMethod(mv, config,
                    FlashlightNames.CONSTRUCTOR_CALL);
            // rethrow exception
            mv.visitInsn(Opcodes.ATHROW);

            // resume
            mv.visitLabel(resume);
        }
    }

    // =========================================================================
    // == Rewrite field accesses
    // =========================================================================

    /**
     * Rewrite a {@code PUTFIELD} instruction.
     * 
     * @param owner
     *            the internal name of the field's owner class (see
     *            {@link Type#getInternalName() getInternalName}).
     * @param name
     *            the field's name.
     * @param desc
     *            the field's descriptor (see {@link Type Type}).
     */
    private void rewritePutfield(final String owner, final String name,
            final String desc) {
        /*
         * If we are in a constructor and we have not yet been initialized then
         * don't instrument the field because we cannot pass the receiver to the
         * Store in an uninitialized state. (The constructors for inner classes
         * have the inits of the "this$" and "val$" fields before the super
         * constructor call.) It's not a big deal that we have missing
         * instrumentation for these fields because they are introduced by the
         * compiler and are not directly accessible by the programmer.
         */
        if (isConstructor && stateMachine != null) {
            currentInsn.accept(mv);
            return;
        }

        final Field field = getField(owner, name);
        if (field != null) {
            /*
             * If we are inside a constructor, the field type is an object or
             * array, and the receiver of the field reference is the receiver of
             * the constructor, we record the initialization
             */
            final int sort = Type.getType(desc).getSort();
            if (isConstructor && (sort == Type.ARRAY || sort == Type.OBJECT)) {
                // Mark the field as referenced, because we are going to use it
                // here
                field.setReferenced();

                /*
                 * We still need to test the receiver. We know we have a
                 * category 1 value because we are dealing with an object.
                 */

                // stack is "..., objectref, value"
                mv.visitInsn(Opcodes.SWAP);
                // stack is "..., value, objectref"
                mv.visitInsn(Opcodes.DUP_X1);
                // stack is "..., objectref, value, objectref"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                // stack is "..., objectref, value, objectref, <rcvr>"

                /*
                 * Jump around the call to instanceFieldInit() if the field is
                 * not from the object being constructed.
                 */
                final Label afterInitCall = new Label();
                mv.visitJumpInsn(Opcodes.IF_ACMPNE, afterInitCall);
                // stack is "..., objectref, value"

                /*
                 * Here we know that the field being assigned is from the object
                 * being constructed, so we need to be record this.
                 * 
                 * We need to copies of both the object and the value.
                 */
                mv.visitInsn(Opcodes.DUP_X1);
                // stack is "..., value, objectref, value"
                mv.visitInsn(Opcodes.SWAP);
                // stack is "..., value, value, objectref"
                mv.visitInsn(Opcodes.DUP_X2);
                // stack is "..., objectref, value, value, objectref"
                mv.visitInsn(Opcodes.SWAP);
                // stack is "..., objectref, value, objectref, value"
                ByteCodeUtils.pushIntegerConstant(mv, field.id);
                // stack is "..., objectref, value, objectref, value, fieldId"
                mv.visitInsn(Opcodes.SWAP);
                // stack is "..., objectref, value, objectref, fieldId, value"
                ByteCodeUtils.callStoreMethod(mv, config,
                        FlashlightNames.INSTANCE_FIELD_INIT);
                // stack is "..., objectref, value"

                mv.visitLabel(afterInitCall);
                // stack is "..., objectref, value"
            }

            if (instrumentField(field)) {
                // Mark the field as referenced
                field.setReferenced();

                /*
                 * We call the store before the field access so that that in the
                 * case of a volatile field the store can get the time stamp of
                 * the access. (We call the store after field reads, but before
                 * field writes, so that happens-before information can be
                 * computed.)
                 */

                /*
                 * We need to manipulate the stack to make a copy of the object
                 * being accessed so that we can have it for the call to the
                 * Store. How we do this depends on whether the top value on the
                 * stack is a category 1 or a category 2 value. We have to test
                 * the type descriptor of the field to determine this.
                 */
                if (ByteCodeUtils.isCategory2(desc)) {
                    // At the start the stack is "..., objectref, value"
                    mv.visitInsn(Opcodes.DUP2_X1);
                    // Stack is "..., value, objectref, value" (+2)
                    mv.visitInsn(Opcodes.POP2);
                    // Stack is "..., value, objectref" (+0)
                    mv.visitInsn(Opcodes.DUP_X2);
                    // Stack is "..., objectref, value, objectref" (+1)
                } else { // Category 1
                    // At the start the stack is "..., objectref, value"
                    mv.visitInsn(Opcodes.SWAP);
                    // Stack is "..., value, objectref" (+0)
                    mv.visitInsn(Opcodes.DUP_X1);
                    // Stack is "..., objectref, value, objectref" (+1)
                }

                /*
                 * Again manipulate the stack so that we can set up the first
                 * two arguments to the Store.fieldAccess() call. The first
                 * argument is a boolean "isRead" flag. The second argument is
                 * the object being accessed.
                 */
                ByteCodeUtils.pushBooleanConstant(mv, false);
                // Stack is "..., objectref, value, objectref, false"
                mv.visitInsn(Opcodes.SWAP);
                // Stack is "..., objectref, value, false, objectref"

                finishFieldAccess(field.id, field.clazz.isInstrumented(),
                        field.clazz.getName(),
                        FlashlightNames.INSTANCE_FIELD_ACCESS);
                // Stack is "..., objectref, value"

                // Execute the original PUTFIELD instruction
                currentInsn.accept(mv);
                // Stack is "..., objectref"
            } else {
                // Execute the original PUTFIELD instruction
                currentInsn.accept(mv);
            }
        } else {
            // Execute the original PUTFIELD instruction
            currentInsn.accept(mv);
        }
    }

    /**
     * Rewrite a {@code GETFIELD} instruction.
     * 
     * @param owner
     *            the internal name of the field's owner class (see
     *            {@link Type#getInternalName() getInternalName}).
     * @param name
     *            the field's name.
     * @param desc
     *            the field's descriptor (see {@link Type Type}).
     */
    private void rewriteGetfield(final String owner, final String name,
            final String desc) {
        final Field field = getField(owner, name);
        if (field != null && instrumentField(field)) {
            // Mark the field as referenced
            field.setReferenced();

            // Stack is "..., objectref"

            /*
             * We need to manipulate the stack to make a copy of the object
             * being accessed so that we can have it for the call to the Store.
             */
            mv.visitInsn(Opcodes.DUP);
            // Stack is "..., objectref, objectref"

            // Execute the original GETFIELD instruction
            currentInsn.accept(mv);
            // Stack is "..., objectref, value" [Value could be cat1 or cat2!]

            /*
             * Again manipulate the stack so that we push the value below the
             * objectref so that we have the objectref for the call to
             * Store.fieldAccess(). Also need to insert "true" for the "isRead"
             * parameter to fieldAccess(). How we do this depends on whether the
             * value is category1 or category2.
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

            finishFieldAccess(field.id, field.clazz.isInstrumented(),
                    field.clazz.getName(),
                    FlashlightNames.INSTANCE_FIELD_ACCESS);
        } else {
            // Execute the original GETFIELD instruction
            currentInsn.accept(mv);
        }
    }

    /**
     * Rewrite a {@code PUTSTATIC} instruction.
     * 
     * @param owner
     *            the internal name of the field's owner class (see
     *            {@link Type#getInternalName() getInternalName}).
     * @param name
     *            the field's name.
     * @param desc
     *            the field's descriptor (see {@link Type Type}).
     */
    private void rewritePutstatic(final String owner, final String name,
            final String desc) {
        final Field field = getField(owner, name);
        if (field != null) {
            /*
             * If we are inside the class initializer, the field is from the
             * class being instrumented, and the type is an object or array, we
             * record the initialization
             */
            if (isClassInitializer
                    && field.clazz.getName().equals(classBeingAnalyzedInternal)) {
                final int sort = Type.getType(desc).getSort();
                if (sort == Type.ARRAY || sort == Type.OBJECT) {
                    // Mark the field as referenced
                    field.setReferenced();

                    // stack is "..., value"
                    mv.visitInsn(Opcodes.DUP);
                    // stack is "..., value, value"
                    ByteCodeUtils.pushIntegerConstant(mv, field.id);
                    // stack is "..., value, value, fieldId"
                    mv.visitInsn(Opcodes.SWAP);
                    // stack is "..., value, fieldId, value"
                    ByteCodeUtils.callStoreMethod(mv, config,
                            FlashlightNames.STATIC_FIELD_INIT);
                    // static is "..., value"
                }
            }

            if (instrumentField(field)) {
                // Mark the field as referenced
                field.setReferenced();

                // Stack is "..., value"

                /*
                 * We call the store before the field access so that that in the
                 * case of a volatile field the store can get the time stamp of
                 * the access. (We call the store after field reads, but before
                 * field writes, so that happens-before information can be
                 * computed.)
                 */
                /*
                 * Push the first arguments on the stack for the call to the
                 * Store.
                 */
                ByteCodeUtils.pushBooleanConstant(mv, false);
                // Stack is "..., value, false"

                finishFieldAccess(field.id, field.clazz.isInstrumented(),
                        field.clazz.getName(),
                        FlashlightNames.STATIC_FIELD_ACCESS);
                // Stack is "..., value"

                // Execute the original PUTSTATIC instruction
                currentInsn.accept(mv);
                // Stack is "..."
            } else {
                // Execute the original PUTSTATIC instruction
                currentInsn.accept(mv);
            }
        } else {
            // Execute the original PUTSTATIC instruction
            currentInsn.accept(mv);
        }
    }

    /**
     * Rewrite a {@code GETSTATIC} instruction.
     * 
     * @param owner
     *            the internal name of the field's owner class (see
     *            {@link Type#getInternalName() getInternalName}).
     * @param name
     *            the field's name.
     * @param desc
     *            the field's descriptor (see {@link Type Type}).
     */
    private void rewriteGetstatic(final String owner, final String name,
            final String desc) {
        // Stack is "..."
        final Field field = getField(owner, name);
        if (field != null && instrumentField(field)) {
            // Mark the field as referenced
            field.setReferenced();

            // Execute the original GETFIELD instruction
            currentInsn.accept(mv);
            // Stack is "..., value" [Value could be cat1 or cat2!]

            /*
             * Manipulate the stack so that we push the first argument to the
             * Store method.
             */
            ByteCodeUtils.pushBooleanConstant(mv, true);
            // Stack is "..., value, true"

            finishFieldAccess(field.id, field.clazz.isInstrumented(),
                    field.clazz.getName(), FlashlightNames.STATIC_FIELD_ACCESS);
        } else {
            // Execute the original GETFIELD instruction
            currentInsn.accept(mv);
        }
    }

    /**
     * Insert code that generates field write events. This is called after we
     * process a call to ObjectInputStream.defaultReadObject(), or
     * ObjectInputStream.readFields().
     */
    private void insertFieldWrites() {
        final int fieldsVar = newLocal(FlashlightNames.JAVA_LANG_OBJECT_TYPE);
        final int counterVar = newLocal(Type.INT_TYPE);
        final int fieldVar = newLocal(FlashlightNames.JAVA_LANG_OBJECT_TYPE);
        ByteCodeUtils.insertPostDeserializationFieldWrites(mv, config,
                classBeingAnalyzedInternal, siteId, fieldsVar, counterVar,
                fieldVar);
    }

    /**
     * Get the field id for the given field. If the field id lookup fails, then
     * this inserts code that throws a FlashlightRuntimeError.
     * 
     * @return The field id, or null} if the lookup failed an exception was
     *         inserted into the code.
     */
    private Field getField(final String className, final String fieldName) {
        try {
            return classModel.getFieldID(className, fieldName);
        } catch (final ClassNotFoundException e) {
            throwMissingClass(e.getMissingClass());
            return null;
        } catch (final FieldNotFoundException e) {
            throwMissingField(e.getMissingField(), e.getClassName());
            return null;
        }
    }

    /**
     * Determine if the field access should be instrumented based on filter
     * settings.
     */
    private boolean instrumentField(final Field field) {
        /*
         * I could make the elements in the FieldFilter enumeration have a
         * filter() method, but this would require me to add too much
         * implementation detail into a class that is meant to be used to convey
         * settings information. While this method is ugly, I think using a
         * filter() method on the enumeration would be uglier.
         */
        if (config.fieldFilter == FieldFilter.NONE) {
            // Instrument all fields
            return true;
        } else if (config.fieldFilter == FieldFilter.DECLARATION) {
            // Instrument fields declared in classes in the named packages
            return config.filterPackages.contains(field.clazz.getPackage());
        } else if (config.fieldFilter == FieldFilter.USE) {
            // Instrument fields accessed in classes in the named packages
            return config.filterPackages.contains(packageNameInternal);
        } else {
            throw new IllegalArgumentException("Unknown field filter: "
                    + config.fieldFilter);
        }
    }

    /**
     * All the field access rewrites end in the same general way once the
     * "isRead" and "receiver" (if any) parameters are pushed onto the stack.
     * This pushes the rest of the parameters on the stack and introduces the
     * call to the appropriate Store method.
     * 
     * <p>
     * When called for an instance field the stack should be "..., isRead,
     * receiver". When called for a static field, the stack should be "...,
     * isRead".
     */
    private void finishFieldAccess(final Integer fieldID,
            final boolean isInstrumented,
            final String declaringClassInternalName, final Method storeMethod) {
        // Stack is "..., isRead, [receiver]"

        /* Push the id of the field */
        ByteCodeUtils.pushIntegerConstant(mv, fieldID);
        // Stack is "..., isRead, [receiver], field_id

        /*
         * Push the site identifier.
         */
        pushSiteIdentifier();

        /*
         * If the class that declares the field is instrumented, then we push
         * the class phantom reference and then null. Otherwise we push null and
         * then the Class object for the class.
         */
        if (isInstrumented) {
            /*
             * Bug 1694: If the class whose phantom class object we want is an
             * ancestor of the class being instrumented, then we use a special
             * getter method to get the phantom class object. Otherwise we
             * access the field directly.
             */
            try {
                if (classModel.getClass(classBeingAnalyzedInternal)
                        .isProperSubclassOf(declaringClassInternalName)) {
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            classBeingAnalyzedInternal,
                            FlashlightNames
                                    .getPhantomClassObjectGetterName(declaringClassInternalName),
                            FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_GETTER_DESC,
                            false);
                } else {
                    mv.visitFieldInsn(
                            Opcodes.GETSTATIC,
                            declaringClassInternalName,
                            FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
                            FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC);
                }
            } catch (final ClassNotFoundException e) {
                messenger
                        .warning("Provided classpath is incomplete 5: couldn't find class "
                                + e.getMissingClass());
                // Still generated legal code.
                mv.visitFieldInsn(Opcodes.GETSTATIC,
                        declaringClassInternalName,
                        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
                        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC);
            }
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
            ByteCodeUtils.pushClass(mv, declaringClassInternalName);
        }

        /* We can now call the store method */
        ByteCodeUtils.callStoreMethod(mv, config, storeMethod);
        // Stack is "..."

        // Resume
    }

    // =========================================================================
    // == Rewrite array access
    // =========================================================================

    private void rewriteArrayLoad(final boolean isCat2) {
        // ..., ref, idx
        mv.visitInsn(Opcodes.DUP2);
        // ..., ref, idx, ref, idx

        /* Execute the original instruction */
        currentInsn.accept(mv);
        // ..., ref, idx, value

        if (isCat2) {
            mv.visitInsn(Opcodes.DUP2_X2);
            // ..., value, ref, idx, value
            mv.visitInsn(Opcodes.POP2);
        } else {
            mv.visitInsn(Opcodes.DUP_X2);
            // ..., value, ref, idx, value
            mv.visitInsn(Opcodes.POP);
        }
        // ..., value, ref, idx

        ByteCodeUtils.pushBooleanConstant(mv, true);
        // ..., value, ref, idx, true
        mv.visitInsn(Opcodes.DUP_X2);
        // ..., value, true, ref, idx, true
        mv.visitInsn(Opcodes.POP);
        // ..., value, true, ref, idx
        pushSiteIdentifier();
        // ..., value, true, ref, idx, siteId

        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.ARRAY_ACCESS);
        // ..., value

        /* Resume */
    }

    private void rewriteArrayStore(final boolean isCat2) {
        // ..., ref, idx, value

        if (isCat2) {
            mv.visitInsn(Opcodes.DUP2_X2);
            // ..., value, ref, idx, value
            mv.visitInsn(Opcodes.POP2);
            // ..., value, ref, idx
            mv.visitInsn(Opcodes.DUP2_X2);
            // ..., ref, idx, value, ref, idx
            mv.visitInsn(Opcodes.DUP2_X2);
            // ..., ref, idx, ref, idx, value, ref, idx
        } else {
            mv.visitInsn(Opcodes.DUP_X2);
            // ..., value, ref, idx, value
            mv.visitInsn(Opcodes.POP);
            // ..., value, ref, idx
            mv.visitInsn(Opcodes.DUP2_X1);
            // ..., ref, idx, value, ref, idx
            mv.visitInsn(Opcodes.DUP2_X1);
            // ..., ref, idx, ref, idx, value, ref, idx
        }

        mv.visitInsn(Opcodes.POP2);
        // ..., ref, idx, ref, idx, value

        /* Execute the original instruction */
        currentInsn.accept(mv);
        // ..., ref, idx

        ByteCodeUtils.pushBooleanConstant(mv, false);
        // ..., ref, idx, false
        mv.visitInsn(Opcodes.DUP_X2);
        // ..., false, ref, idx, false
        mv.visitInsn(Opcodes.POP);
        // ..., false, ref, idx
        pushSiteIdentifier();
        // ..., false, ref, idx, siteId

        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.ARRAY_ACCESS);
        // ...

        /* Resume */
    }

    // =========================================================================
    // == Rewrite monitor methods
    // =========================================================================

    @SuppressWarnings("synthetic-access")
    private void rewriteMonitorenter() {
        if (previousStoreInsn != null) {
            /*
             * There was an ASTORE immediately preceding this monitorenter. We
             * want to delay the output of the ASTORE until immediately before
             * the monitorenter that we output. In this case the stack is
             * already "..., obj, obj"
             */
            // ..., obj, obj (+0,)
        } else {
            /* Copy the lock object to use for comparison purposes */
            // ..., obj
            mv.visitInsn(Opcodes.DUP);
            // ..., obj, obj (,+1)
        }

        ByteCodeUtils.pushLockIsThis(mv, isStatic);
        // ..., obj, obj, isThis (+1, +2)

        /* Compare the object being locked against the Class object */
        mv.visitInsn(Opcodes.SWAP);
        // ..., obj, isThis, obj (+1, +2)
        mv.visitInsn(Opcodes.DUP_X1);
        // ..., obj, obj, isThis, obj (+2, +3)
        ByteCodeUtils.pushClass(mv, classBeingAnalyzedInternal);
        // ..., obj, obj, isThis, obj, inClass (+3, +4)
        final Label pushFalse2 = new Label();
        final Label afterPushIsClass = new Label();
        mv.visitJumpInsn(Opcodes.IF_ACMPNE, pushFalse2);
        // ..., obj, obj, isThis (+1, +2)
        ByteCodeUtils.pushBooleanConstant(mv, true);
        // ..., obj, obj, isThis, true (+2, +3)
        mv.visitJumpInsn(Opcodes.GOTO, afterPushIsClass);
        // END
        mv.visitLabel(pushFalse2);
        // ..., obj, obj, isThis (+1, +2)
        ByteCodeUtils.pushBooleanConstant(mv, false);
        // ..., obj, obj, isThis, false (+2, +3)
        mv.visitLabel(afterPushIsClass);
        // ..., obj, obj, isThis, isClass (+2, +3)

        /* Push the site identifier and call the pre-method */
        pushSiteIdentifier();
        // ..., obj, obj, isThis, isClass, siteId (+4, +5)
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION);
        // ..., obj (-1, 0)

        /* Duplicate the lock object to have it for the post-synchronized method */
        mv.visitInsn(Opcodes.DUP);
        // ..., obj, obj (0, +1)

        if (previousStoreInsn != null) {
            /* Duplicate again, and store it in the local variable */
            mv.visitInsn(Opcodes.DUP);
            // ..., obj, obj, obj (+1,)

            previousStoreInsn.accept(mv);
            previousStoreInsn = null;
            // ..., obj, obj (0,)
        }

        // ..., obj, obj (0, +1)

        /* The original monitor enter call */
        currentInsn.accept(mv);
        // ..., obj (-1, 0)

        /*
         * To make the JIT compiler happy, we must start the try-block
         * immediately after the monitorenter operation. This means we must
         * delay the insertion of our operations to call the post-monitorenter
         * logging call until after the label that follows the monitorenter.
         */
        /*
         * Save the original site identifier, in case it changes before the
         * delayed code is output.
         */
        final long originalSiteId = siteId;
        delayForLabel(new DelayedOutput() {
            @Override
            public void insertCode() {
                /* Push the site identifier and call the post-method */
                ByteCodeUtils.pushLockIsThis(mv, isStatic);
                // ..., obj, lockIsThis
                pushSiteIdentifier(originalSiteId);
                // ..., obj, lockIsThis, siteId
                ByteCodeUtils.callStoreMethod(mv, config,
                        FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION);
                // ...
            }
        });

        /* Resume original instruction stream */
    }

    @SuppressWarnings("synthetic-access")
    private void rewriteMonitorexit() {
        if (previousLoadInsn != null) {
            // ...

            /*
             * There was an ALOAD immediately preceding this monitorexit, but we
             * haven't output it yet. We need two copies of the object, one for
             * the monitorexit, and one for our post-call that follows it, so we
             * need to insert the ALOAD twice. The point here is to make sure
             * that the ALOAD still immediately precedes the monitorexit, which
             * is why we cannot use the DUP operation.
             */
            previousLoadInsn.accept(mv);
            // ..., obj
            previousLoadInsn.accept(mv);
            // ..., obj, obj

            previousLoadInsn = null;
        } else {
            // ..., obj

            /*
             * Copy the object being locked for use as the first parameter to
             * Store.afterInstrinsicLockRelease().
             */
            mv.visitInsn(Opcodes.DUP);
            // ..., obj, obj
        }
        /* The original monitor exit call */
        currentInsn.accept(mv);
        // ..., obj

        /*
         * To make the JIT compiler happy, we must terminate the try-block just
         * after the monitorexit operation. This means we must delay the
         * insertion of the post-monitorexit logging method until after the
         * label that follows the monitorexit instruction is output.
         */
        /*
         * Save the original site id in case the line number changes before the
         * delayed code is output.
         */
        final long originalSiteId = siteId;
        delayForLabel(new DelayedOutput() {
            @Override
            public void insertCode() {
                ByteCodeUtils.pushLockIsThis(mv, isStatic);
                // ..., obj, lockIsThis
                pushSiteIdentifier(originalSiteId);
                // ..., obj, lockIsThis, siteId
                ByteCodeUtils.callStoreMethod(mv, config,
                        FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE);
                // ...
            }
        });

        /* Resume original instruction stream */
    }

    // =========================================================================
    // == Rewrite synchronized methods
    // =========================================================================

    private void pushSynchronizedMethodLockObject() {
        if (isStatic) {
            ByteCodeUtils.pushClass(mv, classBeingAnalyzedInternal);
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
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.BEFORE_INTRINSIC_LOCK_ACQUISITION);
        // empty stack

        /*
         * Insert the explicit monitor acquisition. Even though we already know
         * the lock object by context, we need to store it in a local variable
         * so that we emulate the bytecode produced by the Java compiler. If we
         * don't do this, then the JIT compiler will not compiler the
         * instrumented method to native code. Also, our try-block must start
         * immediately after the monitorenter, and end immediately after the
         * normal monitorexit.
         */
        pushSynchronizedMethodLockObject();
        // lockObj
        mv.visitInsn(Opcodes.DUP);
        // lockObj, lockObj
        syncMethodLockVariable = newLocal(FlashlightNames.JAVA_LANG_OBJECT_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, syncMethodLockVariable);
        // lockObj
        mv.visitInsn(Opcodes.MONITORENTER);
        // empty stack

        final Label startOfTryBlock = new Label();
        endOfTryBlock = new Label();
        startOfExceptionHandler = new Label();
        mv.appendTryCatchBlock(startOfTryBlock, endOfTryBlock,
                startOfExceptionHandler, null);
        mv.visitLabel(startOfTryBlock);

        /* Now call Store.afterIntrinsicLockAcquisition */
        pushSynchronizedMethodLockObject();
        // lockObj
        ByteCodeUtils.pushBooleanConstant(mv, !isStatic);
        // lockObj, isReceiver
        pushSiteIdentifier();
        // lockObj, isReceiver, siteId
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.AFTER_INTRINSIC_LOCK_ACQUISITION);
        // empty stack

        // Resume code
    }

    private void insertSynchronizedMethodPostfix() {
        /*
         * The exception handler also needs an exception handler. A new handler
         * was already started following the last return, and because we are at
         * the end of the method, and all methods must return, this code is part
         * of that handler. The try-block for the handler itself is closed in
         * insertSynchronizedMethodExit().
         */

        /* The exception handler */
        mv.visitLabel(startOfExceptionHandler);

        // exception
        insertSynchronizedMethodExit();
        // exception

        /* Rethrow the exception */
        mv.visitInsn(Opcodes.ATHROW);

        /*
         * Should update the stack depth, but we know we only get executed if
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
        ByteCodeUtils.pushBooleanConstant(mv, !isStatic);
        // ..., lockObj, lockIsThis
        pushSiteIdentifier();
        // ..., lockObj, lockIsThis, siteId
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.AFTER_INTRINSIC_LOCK_RELEASE);
        // ...

        // Resume code

        /*
         * Should update the stack depth, but we know we only get executed if
         * insertSynchronizedMethodPrefix() is run, and that already updates the
         * stack depth by 5, which is more than we need here (3).
         */

        /*
         * New try block is inserted in visitInsn() so that it starts after the
         * return operation.
         */
    }

    // =========================================================================
    // == Rewrite method calls
    // =========================================================================

    private void pushAndStoreTimeStamp(final int timeStampVar) {
        // Get the current nanoTime
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                FlashlightNames.JAVA_LANG_SYSTEM,
                FlashlightNames.NANO_TIME.getName(),
                FlashlightNames.NANO_TIME.getDescriptor(), false);
        // stack is now: nanoTime (long)
        mv.visitVarInsn(Opcodes.LSTORE, timeStampVar);
        // stack is now: <empty>
    }

    private void rewriteMethodCall(final int opcode,
            final IndirectAccessMethod indirectMethod, final String owner,
            final String name, final String desc) {
        final MethodInsnNode methodInsn = (MethodInsnNode) currentInsn;

        final long siteId = siteIdFactory.getSiteId(currentSrcLine, name,
                owner, desc);
        /*
         * Are we dealing with a method that indirectly accesses aggregated
         * state? We must instrument this specially so that they call the Store
         * indirectAccess() method. They use a form of in place instrumentation
         * because we need access to the actual arguments of the method call.
         */
        if (indirectMethod != null) {
            final IndirectAccessMethodInstrumentation methodCall;
            if (opcode == Opcodes.INVOKESTATIC) {
                methodCall = new StaticIndirectAccessMethodInstrumentation(
                        messenger, classModel, happensBefore, siteId,
                        methodInsn, indirectMethod, this);
            } else {
                methodCall = new InstanceIndirectAccessMethodInstrumentation(
                        messenger, classModel, happensBefore, siteId,
                        methodInsn, indirectMethod, this);
            }
            methodCall.popReceiverAndArguments(mv);
            methodCall.recordIndirectAccesses(mv, config);
            methodCall.instrumentMethodCall(mv, isStatic, config);
        } else {
            /*
             * The clone() method is a special case due to its non-standard
             * semantics. If the class of the object being used as the receiver,
             * call it C, implements Cloneable, but DOES NOT override the
             * clone() method, the clone method is still seen as a protected
             * method from Object. This means, we won't be able to invoke the
             * method from a static method in C because the receiver will still
             * be seen as being of type Object. This situation works in JRE 5,
             * but not in JRE 6 due to a stricter bytecode verifier.
             * 
             * The best thing to do in this case is to inline the
             * instrumentation.
             */
            final boolean isClone = opcode == Opcodes.INVOKEVIRTUAL
                    && name.equals("clone") && desc.startsWith("()");

            /*
             * We have encountered code that uses invokevirtual in class D to
             * invoke methods on the receiver inherited from superclass C by
             * naming class C directly as the owner, instead of D as the owner,
             * which is what would normally be done. When this happens, a static
             * wrapper might be illegal depending on the visibility of the
             * method being invoked. So we have to use in-place instrumentation
             * in this case.
             * 
             * Sadly, this casts the net too wide, because we will also use
             * in-place instrumentation for regular objects of class C used
             * within D. It remains to be seen if this is a big deal or not.
             */
            final boolean ownerIsSuper = owner.equals(superClassInternal);

            /*
             * Is the method call annotated? If so, we have to use in-place
             * instrumentation because it's not worth creating a new wrapper
             * method for each distinct annotation of the method.
             */
            final boolean isAnnotated = methodInsn.invisibleTypeAnnotations != null
                    && !methodInsn.invisibleTypeAnnotations.isEmpty()
                    || methodInsn.visibleTypeAnnotations != null
                    && !methodInsn.visibleTypeAnnotations.isEmpty();

            if (inInterface || isClone || ownerIsSuper || isAnnotated) {
                final InPlaceMethodInstrumentation methodCall;
                if (opcode == Opcodes.INVOKESTATIC) {
                    methodCall = new InPlaceStaticMethodInstrumentation(
                            messenger, classModel, happensBefore, siteId,
                            methodInsn);
                } else {
                    methodCall = new InPlaceInstanceMethodInstrumentation(
                            messenger, classModel, happensBefore, siteId,
                            methodInsn, this);
                }
                methodCall.popReceiverAndArguments(mv);
                methodCall.instrumentMethodCall(mv, isStatic, config);
            } else {
                /*
                 * Create the wrapper method information and add it to the list
                 * of wrappers
                 */
                final MethodCallWrapper wrapper;
                if (opcode == Opcodes.INVOKESPECIAL) {
                    wrapper = new SpecialCallWrapper(messenger, classModel,
                            happensBefore, methodInsn);
                } else if (opcode == Opcodes.INVOKESTATIC) {
                    wrapper = new StaticCallWrapper(messenger, classModel,
                            happensBefore, methodInsn);
                } else if (opcode == Opcodes.INVOKEINTERFACE) {
                    wrapper = new InterfaceCallWrapper(messenger, classModel,
                            happensBefore, methodInsn);
                } else { // virtual call
                    wrapper = new VirtualCallWrapper(messenger, classModel,
                            happensBefore, null, methodInsn);
                }

                wrapperMethods.add(wrapper);

                // ..., [objRef], arg1, ..., argN
                pushSiteIdentifier(siteId);
                // ..., [objRef], arg1, ..., argN, siteId
                wrapper.invokeWrapperMethod(mv, classBeingAnalyzedInternal);
                // ..., [returnVlaue]
            }
        }
    }

    // =========================================================================
    // == For implementing IIdObject
    // =========================================================================

    private void initPhantomObjectField() {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.ID_OBJECT,
                FlashlightNames.GET_NEW_ID.getName(),
                FlashlightNames.GET_NEW_ID.getDescriptor(), false);
        ByteCodeUtils.callStoreMethod(mv, config,
                FlashlightNames.GET_OBJECT_PHANTOM);
        mv.visitFieldInsn(Opcodes.PUTFIELD, classBeingAnalyzedInternal,
                FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT,
                FlashlightNames.FLASHLIGHT_PHANTOM_OBJECT_DESC);
    }

    private void updateSiteIdentifier() {
        siteId = siteIdFactory.getSiteId(currentSrcLine);
    }

    private void pushSiteIdentifier(final long id) {
        ByteCodeUtils.pushLongConstant(mv, id);
    }

    private void pushSiteIdentifier() {
        pushSiteIdentifier(siteId);
    }

    private void throwMissingClass(final String missingClassName) {
        messenger.verbose("In Method " + methodName + ": Couldn't find class "
                + missingClassName
                + ".  Inserting code to throw a FlashlightRuntimeError.");
        throwFlashlightRuntimeError(MessageFormat.format(MISSING_CLASS_MESSAGE,
                missingClassName));
    }

    private void throwMissingField(final String missingFieldName,
            final String className) {
        messenger.verbose("In Method " + methodName + ": Couldn't find field "
                + missingFieldName + " in class " + className
                + ".  Inserting code to throw a FlashlightRuntimeError.");
        throwFlashlightRuntimeError(MessageFormat.format(MISSING_FIELD_MESSAGE,
                missingFieldName, className));
    }

    private void throwFlashlightRuntimeError(final String msg) {
        mv.visitTypeInsn(Opcodes.NEW, FlashlightNames.FLASHLIGHT_RUNTIME_ERROR);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                FlashlightNames.FLASHLIGHT_RUNTIME_ERROR,
                FlashlightNames.CONSTRUCTOR, "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
    }

    private boolean callsMethod(final String owner, final Method method,
            final String siteOwner, final String siteName, final String siteDesc)
            throws ClassNotFoundException {
        return classModel.getClass(owner).isAssignableFrom(siteOwner)
                && method.getName().equals(siteName)
                && method.getDescriptor().equals(siteDesc);
    }
}
