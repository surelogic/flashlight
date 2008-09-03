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
 * 
 * @see FlashlightMethodRewriter
 * 
 * @author aarong
 */
public final class FlashlightClassRewriter extends ClassAdapter {
  private static final String UNKNOWN_SOURCE_FILE = "<unknown>";
  
  private static final String CLASS_INITIALIZER = "<clinit>";
  private static final String CLASS_INITIALIZER_DESC = "()V";
  
  private static final int MAX_CODE_SIZE = 64 * 1024;
  
  
  
  /** Properties to control rewriting and instrumentation. */
  private final Configuration config;

  /** Is the current class file an interface? */
  private boolean isInterface;
  
  /** Is the class file version at least Java 5? */
  private boolean atLeastJava5;
  
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
  
  private final FieldIDs fieldIDs;
  
  
  public FlashlightClassRewriter(final FieldIDs fids,
      final Configuration conf, final ClassVisitor cv,
      final Set<MethodIdentifier> ignore) {
    super(cv);
    fieldIDs = fids;
    config = conf;
    methodsToIgnore = ignore;
  }
  
  
  
  
  /**
   * Get the names of those methods whose code size has become too large
   * after instrumentation.
   */
  public Set<MethodIdentifier> getOversizedMethods() {
    final Set<MethodIdentifier> result = new HashSet<MethodIdentifier>();
    for (final Map.Entry<MethodIdentifier, CodeSizeEvaluator> entry : methodSizes.entrySet()) {
      if (entry.getValue().getMaxSize() > MAX_CODE_SIZE) {
        result.add(entry.getKey());
      }
    }
    return Collections.unmodifiableSet(result);
  }
  
  
  
  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    cv.visit(version, access, name, signature, superName, interfaces);
    isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    atLeastJava5 = (version & 0x0000FFFF) >= Opcodes.V1_5;
    classNameInternal = name;
    classNameFullyQualified = ByteCodeUtils.internal2FullyQualified(name);
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
      return FlashlightMethodRewriter.create(fieldIDs,
          access, name, desc, cse, config, atLeastJava5, isInterface,
          sourceFileName, classNameInternal, classNameFullyQualified,
          wrapperMethods);
    }
  }
  
  @Override
  public void visitEnd() {
    if (!atLeastJava5) {
      // insert our new field
      final FieldVisitor fv = cv.visitField(
          isInterface ? FlashlightNames.IN_CLASS_ACCESS_INTERFACE
              : FlashlightNames.IN_CLASS_ACCESS_CLASS,
          FlashlightNames.IN_CLASS, FlashlightNames.IN_CLASS_DESC, null, null);
      fv.visitEnd();

      // Add the class initializer if needed
      if (needsClassInitializer) {
        addClassInitializer();
      }
    }
    
    // Add the wrapper methods
    for (final MethodCallWrapper wrapper : wrapperMethods) {
      addWrapperMethod(wrapper);
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
    final MethodVisitor rewriter_mv = FlashlightMethodRewriter.create(fieldIDs,
        Opcodes.ACC_STATIC, CLASS_INITIALIZER, CLASS_INITIALIZER_DESC, mv,
        config, atLeastJava5, isInterface, sourceFileName,
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
