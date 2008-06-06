package com.surelogic._flashlight.rewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class FlashlightClassRewriter extends ClassAdapter {
  private static final String UNKNOWN_SOURCE_FILE = "<unknown>";
  
  private static final String CLASS_INITIALIZER = "<clinit>";
  private static final String CLASS_INITIALIZER_DESC = "()V";
  

  
  /** Is the current class file an interface? */
  private boolean isInterface;
  
  /** Is the class file version at least Java 5? */
  @SuppressWarnings("unused")
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
  
  
  
  public FlashlightClassRewriter(final ClassVisitor cv) {
    super(cv);
  }
  
  
  
  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName,
      final String[] interfaces) {
    cv.visit(version, access, name, signature, superName, interfaces);
    isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    atLeastJava5 = (version & 0xFFFF0000) >= Opcodes.V1_5;
    classNameInternal = name;
    classNameFullyQualified = Utils.internal2FullyQualified(name);
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
    
    final int newAccess = access & ~Opcodes.ACC_SYNCHRONIZED;
    return new FlashlightMethodRewriter(
        sourceFileName, classNameInternal, classNameFullyQualified,
        name, isClassInit, wrapperMethods, access,
        cv.visitMethod(newAccess, name, desc, signature, exceptions));
  }
  
  @Override
  public void visitEnd() {
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
    
    // Add the wrapper methods
    for (final MethodCallWrapper wrapper : wrapperMethods) {
      addWrapperMethod(wrapper);
    }
    
    // Now we are done
    cv.visitEnd();
  }
  
  
  
  private void addClassInitializer() {
    // XXX: Clean this up, it is error prone
    final MethodVisitor mv =
      cv.visitMethod(Opcodes.ACC_STATIC, CLASS_INITIALIZER,
          CLASS_INITIALIZER_DESC, null, null);
    final MethodVisitor rewriter_mv =
      new FlashlightMethodRewriter(sourceFileName,
        classNameInternal, classNameFullyQualified,
        CLASS_INITIALIZER, true, new HashSet<MethodCallWrapper>(), Opcodes.ACC_STATIC, mv);
    rewriter_mv.visitCode();
    mv.visitInsn(Opcodes.RETURN);
    rewriter_mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
  
  private void addWrapperMethod(final MethodCallWrapper wrapper) {
    /* Create the method header */
    final MethodVisitor mv = wrapper.createMethodHeader(cv);
    mv.visitCode();
    
    /* before method all event */
    // empty stack 
    ByteCodeUtils.pushBooleanConstant(mv, true);
    // true
    wrapper.pushObjectRefForEvent(mv);
    // true, objRef
    mv.visitLdcInsn(sourceFileName);
    // true, objRef, filename
    wrapper.pushCallingMethodName(mv);
    // true, objRef, filename, callingMethodName
    ByteCodeUtils.pushInClass(mv, classNameInternal);
    // true, objRef, filename, callingMethodName, inClass
    wrapper.pushCallingLineNumber(mv);
    // true, objRef, filename, callingMethodName, inClass, line
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE, FlashlightNames.METHOD_CALL, FlashlightNames.METHOD_CALL_SIGNATURE);
    // empty stack 

    /* Additional pre-call instrumentation */
    insertBeforeAnyJUCLockAcquisition(mv, wrapper);
    
    final Label beforeOriginalCall = new Label();
    final Label afterOriginalCall = new Label();
    final Label exceptionHandler = new Label();
    mv.visitTryCatchBlock(beforeOriginalCall, afterOriginalCall, exceptionHandler, null);
    
    /* original method call */
    wrapper.pushObjectRefForOriginalMethod(mv);
    // objRef
    wrapper.pushOriginalArguments(mv);
    // objRef, arg1, ..., argN

    mv.visitLabel(beforeOriginalCall);
    wrapper.invokeOriginalMethod(mv);
    mv.visitLabel(afterOriginalCall);
    // [returnValue]

    /* Additional post-call instrumentation */
    insertAfterLockAndLockInterruptibly(mv, wrapper, true);
    insertAfterTryLockNormal(mv, wrapper);
    insertAfterUnlock(mv, wrapper, true);
    
    /* after method call event */
    ByteCodeUtils.pushBooleanConstant(mv, false);
    // [returnValue], true
    wrapper.pushObjectRefForEvent(mv);
    // [returnValue], true, objRef
    mv.visitLdcInsn(sourceFileName);
    // [returnValue], true, objRef, filename
    wrapper.pushCallingMethodName(mv);
    // [returnValue], true, objRef, filename, callingMethodName
    ByteCodeUtils.pushInClass(mv, classNameInternal);
    // [returnValue], true, objRef, filename, callingMethodName, inClass
    wrapper.pushCallingLineNumber(mv);
    // [returnValue], true, objRef, filename, callingMethodName, inClass, line
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE, FlashlightNames.METHOD_CALL, FlashlightNames.METHOD_CALL_SIGNATURE);
    // [returnValue]
    
    /* Method return */
    wrapper.methodReturn(mv);

    /* Exception handler: still want to report method exit when there is an exception */
    mv.visitLabel(exceptionHandler);
    // exception

    /* Additional post-call instrumentation */
    insertAfterLockAndLockInterruptibly(mv, wrapper, false);
    insertAfterTryLockException(mv, wrapper);
    insertAfterUnlock(mv, wrapper, false);
    
    /* after method call event */
    ByteCodeUtils.pushBooleanConstant(mv, false);
    // exception, true
    wrapper.pushObjectRefForEvent(mv);
    // exception, true, objRef
    mv.visitLdcInsn(sourceFileName);
    // exception, true, objRef, filename
    wrapper.pushCallingMethodName(mv);
    // exception, true, objRef, filename, callingMethodName
    ByteCodeUtils.pushInClass(mv, classNameInternal);
    // exception, true, objRef, filename, callingMethodName, inClass
    wrapper.pushCallingLineNumber(mv);
    // exception, true, objRef, filename, callingMethodName, inClass, line
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE, FlashlightNames.METHOD_CALL, FlashlightNames.METHOD_CALL_SIGNATURE);
    // exception
    mv.visitInsn(Opcodes.ATHROW);
    
    final int numLocals = wrapper.getNumLocals();
    mv.visitMaxs(Math.max(6 + Math.max(1, wrapper.getMethodReturnSize()), numLocals), numLocals);
    mv.visitEnd();
  }
  
//  private void insertBeforeWaitCall(
//      final MethodVisitor mv, final WrapperMethod wrapper) {
//    if (wrapper.testOriginalName(
//        FlashlightNames.JAVA_LANG_OBJECT, FlashlightNames.WAIT)) {
//      // ...
//      ByteCodeUtils.pushBooleanConstant(mv, true);
//      // ..., true
//      wrapper.pushObjectRef(mv);
//      // ..., true, objRef
//      ByteCodeUtils.pushInClass(mv, classNameInternal);
//      // ..., true, objRef, inClass
//      wrapper.pushCallingLineNumber(mv);
//      // ..., true, objRef, inClass, line
//      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE, FlashlightNames.INTRINSIC_LOCK_WAIT, FlashlightNames.INTRINSIC_LOCK_WAIT_SIGNATURE);      
//    }
//  }
  
//  private void insertAfterWaitCall(
//      final MethodVisitor mv, final WrapperMethod wrapper) {
//    if (wrapper.testOriginalName(
//        FlashlightNames.JAVA_LANG_OBJECT, FlashlightNames.WAIT)) {
//      // ...
//      ByteCodeUtils.pushBooleanConstant(mv, false);
//      // ..., true
//      wrapper.pushObjectRef(mv);
//      // ..., true, objRef
//      ByteCodeUtils.pushInClass(mv, classNameInternal);
//      // ..., true, objRef, inClass
//      wrapper.pushCallingLineNumber(mv);
//      // ..., true, objRef, inClass, line
//      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE, FlashlightNames.INTRINSIC_LOCK_WAIT, FlashlightNames.INTRINSIC_LOCK_WAIT_SIGNATURE);      
//    }
//  }
  
  private void insertBeforeAnyJUCLockAcquisition(
      final MethodVisitor mv, final MethodCallWrapper wrapper) {
    if (wrapper.testOriginalName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK, FlashlightNames.LOCK)
        || wrapper.testOriginalName(
            FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
            FlashlightNames.LOCK_INTERRUPTIBLY)
        || wrapper.testOriginalName(
            FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
            FlashlightNames.TRY_LOCK)) {
      // ...
      wrapper.pushObjectRefForOriginalMethod(mv);
      // ..., objRef
      ByteCodeUtils.pushInClass(mv, classNameInternal);
      // ..., objRef, inClass
      wrapper.pushCallingLineNumber(mv);
      // ..., objRef, inClass, line
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
          FlashlightNames.BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT,
          FlashlightNames.BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE);
    }
  }
  
  private void insertAfterLockAndLockInterruptibly(final MethodVisitor mv,
      final MethodCallWrapper wrapper, final boolean gotTheLock) {
    if (wrapper.testOriginalName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK, FlashlightNames.LOCK)
        || wrapper.testOriginalName(
            FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
            FlashlightNames.LOCK_INTERRUPTIBLY)) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, gotTheLock);
      // ..., gotTheLock
      wrapper.pushObjectRefForOriginalMethod(mv);
      // ..., gotTheLock, objRef
      ByteCodeUtils.pushInClass(mv, classNameInternal);
      // ..., gotTheLock, objRef, inClass
      wrapper.pushCallingLineNumber(mv);
      // ..., gotTheLock, objRef, inClass, line
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE);      
    }
  }
  
  private void insertAfterTryLockNormal(
      final MethodVisitor mv, final MethodCallWrapper wrapper) {
    if (wrapper.testOriginalName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
        FlashlightNames.TRY_LOCK)) {
      // ..., gotTheLock
      
      /* We need to make a copy of tryLock()'s return value */
      mv.visitInsn(Opcodes.DUP);
      // ..., gotTheLock, gotTheLock      
      wrapper.pushObjectRefForOriginalMethod(mv);
      // ..., gotTheLock, gotTheLock, objRef
      ByteCodeUtils.pushInClass(mv, classNameInternal);
      // ..., gotTheLock, gotTheLock, objRef, inClass
      wrapper.pushCallingLineNumber(mv);
      // ..., gotTheLock, gotTheLock, objRef, inClass, line
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE);
      // ..., gotTheLock
    }
  }
  
  private void insertAfterTryLockException(
      final MethodVisitor mv, final MethodCallWrapper wrapper) {
    if (wrapper.testOriginalName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
        FlashlightNames.TRY_LOCK)) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., false
      wrapper.pushObjectRefForOriginalMethod(mv);
      // ..., false, objRef
      ByteCodeUtils.pushInClass(mv, classNameInternal);
      // ..., false, objRef, inClass
      wrapper.pushCallingLineNumber(mv);
      // ..., false, objRef, inClass, line
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT_SIGNATURE);      
    }
  }
  
  
  private void insertAfterUnlock(final MethodVisitor mv,
      final MethodCallWrapper wrapper, final boolean releasedTheLock) {
    if (wrapper.testOriginalName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
        FlashlightNames.UNLOCK)) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, releasedTheLock);
      // ..., gotTheLock
      wrapper.pushObjectRefForOriginalMethod(mv);
      // ..., gotTheLock, objRef
      ByteCodeUtils.pushInClass(mv, classNameInternal);
      // ..., gotTheLock, objRef, inClass
      wrapper.pushCallingLineNumber(mv);
      // ..., gotTheLock, objRef, inClass, line
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.FLASHLIGHT_STORE,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT,
          FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT_SIGNATURE);      
    }
  }


  
  // ========================================================================
  
  
  
  public static void main(final String[] args) throws IOException {
    rewriteDirectory(new File(args[0]), new File(args[1]));
    
    System.out.println("done");
  }

  private static void rewriteDirectory(final File inDir, final File outDir)
      throws FileNotFoundException, IOException {
    for (final String name : inDir.list()) {
      final File nextIn = new File(inDir, name);
      final File nextOut = new File(outDir, name);
      if (nextIn.isDirectory()) {
        rewriteDirectory(nextIn, nextOut);
      } else {
        if (name.endsWith(".class")) {
          rewriteClass(nextIn, nextOut);
        }
      }
    }
  }


  private static void rewriteClass(final File inName, final File outName)
      throws FileNotFoundException, IOException {
    System.out.println("Reading class " + inName);
    final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inName));
    final ClassReader input = new ClassReader(bis);
    final ClassWriter output = new ClassWriter(input, 0);
    final FlashlightClassRewriter xformer = new FlashlightClassRewriter(output);
    input.accept(xformer, 0);
    bis.close();
    
    final byte[] newClass = output.toByteArray();    

    System.out.println("Writing class " + outName);
    final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outName));
    bos.write(newClass);
    bos.close();
  }
}
