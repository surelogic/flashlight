package com.surelogic._flashlight.rewriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;

/**
 * Visits a classfile and rewrites it to contain Flashlight instrumentation.
 * This is the second pass of the instrumentation process. The first pass is
 * implemented by {@link FieldCataloger}, and the results of the first pass are
 * provided to this pass as a {@link ClassAndFieldModel} instance. This class
 * can be used as the first and only pass by providing an empty class model. In
 * this case, all the field accesses will use reflection at runtime, which has
 * significant runtime costs.
 * 
 * <p>
 * Most of the real work is performed by instances of
 * {@link FlashlightMethodRewriter}.
 * 
 * @see FieldCataloger
 * @see ClassAndFieldModel
 * @see FlashlightMethodRewriter
 * @see Configuration
 */
final class FlashlightClassRewriter extends ClassAdapter {
  private static final String UNKNOWN_SOURCE_FILE = "<unknown>";
  
  private static final String CLASS_INITIALIZER = "<clinit>";
  private static final String CLASS_INITIALIZER_DESC = "()V";
  
  /**
   * The maximum size in bytes that a method code section is allowed to be.
   * Methods that end up larger than this after instrumentation are not 
   * instrumented at all.
   */
  private static final int MAX_CODE_SIZE = 64 * 1024;
  
  
  
  /** Properties to control rewriting and instrumentation. */
  private final Configuration config;

  /** Messenger for status reports. */
  private final EngineMessenger messenger;
  
  /** Is the current class file an interface? */
  private boolean isInterface;
  
  /** Is the class file version at least Java 5? */
  private boolean atLeastJava5;
  
  /**
   * Should the super constructor call be updated from {@code java.lang.Object}
   * to {@code com.surelogic._flashlight.rewriter.runtime.IdObject}.
   */
  private boolean updateSuperCall;
  
  /** The name of the source file that contains the class being rewritten. */
  private String sourceFileName = UNKNOWN_SOURCE_FILE;

  /** The internal name of the class being rewritten. */
  private String classNameInternal;
  
  /** The fully qualified name of the class being rewritten. */
  private String classNameFullyQualified;
  
  /**
   * Do we need to add a class initializer?  If the class already had one,
   * we modify it.  Otherwise we need to add one.
   */
  private boolean needsClassInitializer = true;
  
  /**
   * The wrapper methods that we need to generate to instrument calls to
   * instance methods.
   */
  private final Set<MethodCallWrapper> wrapperMethods =
    new TreeSet<MethodCallWrapper>(MethodCallWrapper.comparator);
  
  /**
   * Table from method names to CodeSizeEvaluators.  Need to look at this
   * after the class has been visited to find overly long methods.
   * The key is the method name + method description.
   */
  private final Map<MethodIdentifier, CodeSizeEvaluator> methodSizes =
    new HashMap<MethodIdentifier, CodeSizeEvaluator>();
  
  /**
   * The set of methods that should not be rewritten.
   */
  private final Set<MethodIdentifier> methodsToIgnore;
  
  /**
   * After the entire class has been visited this contains the names of all
   * the methods that are oversized after instrumentation.
   */
  private final Set<MethodIdentifier> oversizedMethods = new HashSet<MethodIdentifier>();
  
  /**
   * The class and field model built during the first pass.  This is used
   * to obtain unique field identifiers.  For a field that does not have a
   * unique identifier in this model, we must use reflection at runtime.
   */
  private final ClassAndFieldModel classModel;
  
  
  
  /**
   * Create a new class rewriter.
   * 
   * @param conf
   *          The configuration information for instrumentation.
   * @param cv
   *          The class visitor to delegate to.
   * @param model
   *          The class and field model to use.
   * @param ignore
   *          The set of methods that <em>should not</em> be instrumented.
   *          This are determined by a prior attempt at instrumentation, and would
   *          be obtained by calling {@link #getOversizedMethods()}.
   */
  public FlashlightClassRewriter(final Configuration conf,
      final EngineMessenger msg,
      final ClassVisitor cv, final ClassAndFieldModel model,
      final Set<MethodIdentifier> ignore) {
    super(cv);
    config = conf;
    messenger = msg;
    classModel = model;
    methodsToIgnore = ignore;
  }
  
  
  
  
  /**
   * Get the names of those methods whose code size has become too large
   * after instrumentation.
   */
  public Set<MethodIdentifier> getOversizedMethods() {
    return Collections.unmodifiableSet(oversizedMethods);
  }
  
  
  
  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    atLeastJava5 = (version & 0x0000FFFF) >= Opcodes.V1_5;
    classNameInternal = name;
    classNameFullyQualified = ByteCodeUtils.internal2FullyQualified(name);

    final String newSuperName;
    if (!isInterface && superName.equals(FlashlightNames.JAVA_LANG_OBJECT)) {
      newSuperName = FlashlightNames.ID_OBJECT;
      updateSuperCall = true;
    } else {
      newSuperName = superName;
      updateSuperCall = false;
    }
    
    /* If the class extends from java.lang.Object, we change it to extend
     * com.surelogic._flashlight.rewriter.runtime.IdObject.
     */
    cv.visit(version, access, name, signature, newSuperName, interfaces);
  }

  @Override
  public void visitSource(final String source, final String debug) {
    if (source != null) {
      sourceFileName = source;
    }
    cv.visitSource(source, debug);
  }
  
  @Override
  public MethodVisitor visitMethod(final int access, final String name,
      final String desc, final String signature, final String[] exceptions) {
    final boolean isClassInit = name.equals(CLASS_INITIALIZER);
    if (isClassInit) {
      needsClassInitializer = false;
    }
    
    final MethodIdentifier methodId = new MethodIdentifier(name, desc);
    if (methodsToIgnore.contains(methodId)) {
      return cv.visitMethod(access, name, desc, signature, exceptions);
    } else {
      final int newAccess;
      if (config.rewriteSynchronizedMethod) {
        newAccess = access & ~Opcodes.ACC_SYNCHRONIZED;
      } else {
        newAccess = access;
      }
      final MethodVisitor original =
        cv.visitMethod(newAccess, name, desc, signature, exceptions);
      final CodeSizeEvaluator cse = new CodeSizeEvaluator(original);
      methodSizes.put(methodId, cse);
      return FlashlightMethodRewriter.create(access,
          name, desc, cse, config, messenger, classModel, atLeastJava5, isInterface,
          updateSuperCall, sourceFileName, classNameInternal, classNameFullyQualified,
          wrapperMethods);
    }
  }
  
  @Override
  public void visitEnd() {
    // Insert the withinClass field (always) and inClass field (when needed)
    FieldVisitor fv = cv.visitField(
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_ACCESS,
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT,
        FlashlightNames.FLASHLIGHT_PHANTOM_CLASS_OBJECT_DESC, null, null);
    fv.visitEnd();
   
    fv = cv.visitField(
        FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO_ACCESS,
        FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO,
        FlashlightNames.FLASHLIGHT_CLASS_LOADER_INFO_DESC, null, null);
    fv.visitEnd();
    
    if (!atLeastJava5) {
      // insert our new field
      fv = cv.visitField(
          FlashlightNames.FLASHLIGHT_CLASS_OBJECT_ACCESS,
          FlashlightNames.FLASHLIGHT_CLASS_OBJECT, FlashlightNames.FLASHLIGHT_CLASS_OBJECT_DESC,
          null, null);
      fv.visitEnd();
    }

    // Add the class initializer if needed
    if (needsClassInitializer) {
      addClassInitializer();
    }
    
    // Add the wrapper methods
    for (final MethodCallWrapper wrapper : wrapperMethods) {
      addWrapperMethod(wrapper);
    }
    
    // Find any oversized methods
    for (final Map.Entry<MethodIdentifier, CodeSizeEvaluator> entry : methodSizes.entrySet()) {
      if (entry.getValue().getMaxSize() > MAX_CODE_SIZE) {
        final MethodIdentifier mid = entry.getKey();
        oversizedMethods.add(mid);
        messenger.warning("Instrumentation causes method "
            + classNameFullyQualified + "." + mid.name + mid.desc
            + " to be too large.");
      }
    }
    
    // Now we are done
    cv.visitEnd();
  }
  
  
  
  private void addClassInitializer() {
    /* Create a new <clinit> method to vist */
    final MethodVisitor mv =
      cv.visitMethod(Opcodes.ACC_STATIC, CLASS_INITIALIZER,
          CLASS_INITIALIZER_DESC, null, null);
    /* Proceed as if visitMethod() were called on us, and simulate the method
     * traversal through the rewriter visitor.
     */
    final MethodVisitor rewriter_mv = FlashlightMethodRewriter.create(Opcodes.ACC_STATIC,
        CLASS_INITIALIZER, CLASS_INITIALIZER_DESC, mv, config, messenger,
        classModel, atLeastJava5, isInterface, updateSuperCall, sourceFileName,
        classNameInternal, classNameFullyQualified, wrapperMethods);
    rewriter_mv.visitCode(); // start code section
    rewriter_mv.visitInsn(Opcodes.RETURN); // empty method, just return
    rewriter_mv.visitMaxs(0, 0); // Don't need any stack or variables
    rewriter_mv.visitEnd(); // end of method
  }
  
  
  
  private void addWrapperMethod(final MethodCallWrapper wrapper) {
    /* Create the method header */
    final MethodVisitor mv = wrapper.createMethodHeader(cv);
    mv.visitCode();
    
    // empty stack

    /* Instrument the method call */
    final MethodCallInstrumenter instrumenter = new MethodCallInstrumenter(
        config, mv, wrapper, atLeastJava5, sourceFileName, classNameInternal);
    instrumenter.instrumentMethodCall();
    
    /* Return from method */
    wrapper.methodReturn(mv);

    final int numLocals = wrapper.getNumLocals();
    mv.visitMaxs(
        Math.max(6 + Math.max(1, wrapper.getMethodReturnSize()), numLocals),
        numLocals);
    mv.visitEnd();
  }
}
