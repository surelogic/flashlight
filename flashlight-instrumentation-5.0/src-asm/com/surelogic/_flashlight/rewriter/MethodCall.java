package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.config.Configuration;
import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig.HappensBefore;
import com.surelogic._flashlight.rewriter.config.HappensBeforeConfig.ReturnCheck;

/**
 * Abstract representation of a method call that needs to be instrumented.
 * Generic enough to accommodate both method calls that are instrumented by being
 * placed in wrapper methods and to and method calls that are instrumented 
 * in place (in the case of calls from interface initializers).
 */
public abstract class MethodCall {
  protected final int opcode;
  protected final String owner;
  protected final String name;
  protected final String descriptor;
  protected final Type returnType;
  
  protected final RewriteMessenger messenger;
  protected final ClassAndFieldModel classModel;
  
  
  /**
   * @param opcode The opcode used to invoke the method.
   * @param owner The owner of the method.
   * @param originalName The name of the method.
   * @param originalDesc The descriptor of the method.
   */
  public MethodCall(final RewriteMessenger msg,
      final ClassAndFieldModel model, final int opcode, final String owner,
      final String originalName, final String originalDesc) {
    this.opcode = opcode;
    this.owner = owner;
    this.name = originalName;
    this.descriptor = originalDesc;
    this.returnType = Type.getReturnType(originalDesc);
    
    messenger = msg;
    classModel = model;
  }

  
  
  private final boolean testCalledMethodName(
      final String testOwner, final String testName)
  throws ClassNotFoundException {
    /* Test the method name first: no sense fooling with the class model if
     * the method doesn't match.
     */
    return name.equals(testName) && classModel.getClass(testOwner).isAssignableFrom(owner);
  }

  /**
   * Push the call site identifier.
   */
  public abstract void pushSiteId(MethodVisitor mv);
  
  /**
   * Pushes the receiver object on to the stack for use by an instrumentation
   * event method.  Must push <code>null</code> if the method is static.
   * @param mv
   */
  public abstract void pushReceiverForEvent(MethodVisitor mv);
  
  /**
   * Pushes the given argument on to the stack for use by an instrumentation
   * event method.
   * 
   * @param arg
   *          A number greater than 0, where 1 indicates the first non-receiver
   *          argument, 2 the second non-receiver argument, etc.
   */
  public abstract void pushArgumentForEvent(MethodVisitor mv, int arg);
  
  /**
   * Push the original receiver and original arguments onto the stack so that
   * method can be invoked.  This is separate from invoking the method via
   * {@link #invokeMethod(MethodVisitor)} so that the labels for the try-finally
   * block can be inserted around the method invocation.  This keeps the block
   * from being larger than necessary.
   */
  public abstract void pushReceiverAndArguments(MethodVisitor mv);
  
  /**
   * Get the return type of the method;
   */
  public Type getReturnType() {
    return returnType;
  }
  
  public final void invokeMethod(final MethodVisitor mv) {
    mv.visitMethodInsn(opcode, owner, name, descriptor);
  }

  /**
   * Insert the original method call surrounded by the appropriate 
   * instrumentation.
   */
  public final void instrumentMethodCall(
      final ExceptionHandlerReorderingMethodAdapter mv, final Configuration config) {
    // ...
    
    /* before method all event */
    instrumentBeforeMethodCall(mv, config); // +6
    // ...

    /* Additional pre-call instrumentation */
    instrumentBeforeAnyJUCLockAcquisition(mv, config); // +3
    instrumentBeforeWait(mv, config); // +4
    // ...

    final Label beforeOriginalCall = new Label();
    final Label afterOriginalCall = new Label();
    final Label exceptionHandler = new Label();
    final Label resume = new Label();
    mv.prependTryCatchBlock(beforeOriginalCall, afterOriginalCall, exceptionHandler, null);
    
    /* original method call */
    this.pushReceiverAndArguments(mv); // +X
    // rcvr, arg1, ..., argN

    mv.visitLabel(beforeOriginalCall);
    this.invokeMethod(mv);
    mv.visitLabel(afterOriginalCall);
    // [returnValue]

    /* Additional post-call instrumentation */
    instrumentAfterLockAndLockInterruptibly(mv, config, true); // +4
    instrumentAfterTryLockNormal(mv, config); // +4
    instrumentAfterUnlock(mv, config, true); // +4
    instrumentAfterWait(mv, config); // +4
    
    instrumentHappensBefore(mv, config);
    
    // XXX: Not doing this yet
//    instrumentAfterTryMethod(mv, config);
    
    /* after method call event */
    instrumentAfterMethodCall(mv, config); // +6
    
    /* Jump around exception handler */
    mv.visitJumpInsn(Opcodes.GOTO, resume);

    /* Exception handler: still want to report method exit when there is an exception */
    mv.visitLabel(exceptionHandler);
    // exception

    /* Additional post-call instrumentation */
    instrumentAfterLockAndLockInterruptibly(mv, config, false); // +4 (+1 exception)
    instrumentAfterTryLockException(mv, config); // +4 (+1 exception)
    instrumentAfterUnlock(mv, config, false); // +4 (+1 exception)
    instrumentAfterWait(mv, config); // +4 (+1 exception)
    // If there was an exception then the "try" method fails, so nothing to report
    
    instrumentAfterMethodCall(mv, config); // +6 (+1 exception)

    // exception
    mv.visitInsn(Opcodes.ATHROW);

    /* Resume normal execution */
    mv.visitLabel(resume);
  }


  // +4 Stack
  private void instrumentBeforeMethodCall(final MethodVisitor mv, final Configuration config) {
    if (config.instrumentBeforeCall) {
      // empty stack 
      ByteCodeUtils.pushBooleanConstant(mv, true);
      // true
      this.pushReceiverForEvent(mv);
      // true, objRef
      this.pushSiteId(mv);
      // true, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.METHOD_CALL);
      // empty stack 
    }
  }

  // +4 Stack
  private void instrumentAfterMethodCall(final MethodVisitor mv, final Configuration config) {
    if (config.instrumentAfterCall) {
      // ...,
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., false
      this.pushReceiverForEvent(mv);
      // ..., false, objRef
      this.pushSiteId(mv);
      // ..., false, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.METHOD_CALL);
      // ...
    }
  }

  // +4 Stack
  private void instrumentBeforeWait(
      final MethodVisitor mv, final Configuration config) {
    try {
      if (this.testCalledMethodName(
          FlashlightNames.JAVA_LANG_OBJECT, FlashlightNames.WAIT)
          && config.instrumentBeforeWait) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, true);
        // ..., true
        this.pushReceiverForEvent(mv);
        // ..., true, objRef
        this.pushSiteId(mv);
        // ..., true, objRef, callSiteId (long)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INTRINSIC_LOCK_WAIT);      
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }
  
  // +4 Stack
  private void instrumentAfterWait(
      final MethodVisitor mv, final Configuration config) {
    try {
      if (this.testCalledMethodName(
          FlashlightNames.JAVA_LANG_OBJECT, FlashlightNames.WAIT)
          && config.instrumentAfterWait) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, false);
        // ..., false
        this.pushReceiverForEvent(mv);
        // ..., false, objRef
        this.pushSiteId(mv);
        // ..., false, objRef, callSiteId (long)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INTRINSIC_LOCK_WAIT);
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }
  
  // +3 Stack
  private void instrumentBeforeAnyJUCLockAcquisition(
      final MethodVisitor mv, final Configuration config) {
    try {
      if ((this.testCalledMethodName(
          FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK, FlashlightNames.LOCK)
          || this.testCalledMethodName(
              FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
              FlashlightNames.LOCK_INTERRUPTIBLY)
          || this.testCalledMethodName(
              FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
              FlashlightNames.TRY_LOCK))
          && config.instrumentBeforeJUCLock) {
        // ...
        this.pushReceiverForEvent(mv);
        // ..., objRef
        this.pushSiteId(mv);
        // ..., objRef, callSiteId (long)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }
  
  // +4 Stack
  private void instrumentAfterLockAndLockInterruptibly(final MethodVisitor mv,
      final Configuration config, final boolean gotTheLock) {
    try {
      if ((this.testCalledMethodName(
          FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK, FlashlightNames.LOCK)
          || this.testCalledMethodName(
              FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
              FlashlightNames.LOCK_INTERRUPTIBLY))
          && config.instrumentAfterLock) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, gotTheLock);
        // ..., gotTheLock
        this.pushReceiverForEvent(mv);
        // ..., gotTheLock, objRef
        this.pushSiteId(mv);
        // ..., gotTheLock, objRef, callSiteId (long)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);      
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }
  
  // +4 Stack
  private void instrumentAfterTryLockNormal(
      final MethodVisitor mv, final Configuration config) {
    try {
      if (this.testCalledMethodName(
          FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
          FlashlightNames.TRY_LOCK) && config.instrumentAfterTryLock) {
        // ..., gotTheLock
        
        /* We need to make a copy of tryLock()'s return value */
        mv.visitInsn(Opcodes.DUP);
        // ..., gotTheLock, gotTheLock      
        this.pushReceiverForEvent(mv);
        // ..., gotTheLock, gotTheLock, objRef
        this.pushSiteId(mv);
        // ..., gotTheLock, gotTheLock, objRef, callSiteId (long)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);
        // ..., gotTheLock
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }
  
  //+4 Stack
  private void instrumentAfterTryLockException(
      final MethodVisitor mv, final Configuration config) {
    try {
      if (this.testCalledMethodName(
          FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
          FlashlightNames.TRY_LOCK) && config.instrumentAfterTryLock) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, false);
        // ..., false
        this.pushReceiverForEvent(mv);
        // ..., false, objRef
        this.pushSiteId(mv);
        // ..., false, objRef, callSiteId (false)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);      
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }
  
  // +4 Stack
  private void instrumentAfterUnlock(
      final MethodVisitor mv, final Configuration config,
      final boolean releasedTheLock) {
    try {
      if (this.testCalledMethodName(
          FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
          FlashlightNames.UNLOCK) && config.instrumentAfterUnlock) {
        // ...
        ByteCodeUtils.pushBooleanConstant(mv, releasedTheLock);
        // ..., gotTheLock
        this.pushReceiverForEvent(mv);
        // ..., gotTheLock, objRef
        this.pushSiteId(mv);
        // ..., gotTheLock, objRef, callSiteId (long)
        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT);
      }
    } catch (final ClassNotFoundException e) {
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }
  }  
  
//  private void instrumentAfterTryMethod(
//      final MethodVisitor mv, final Configuration config) {
//    try {
//      if (this.testCalledMethodName(
//          "java/util/concurrent/Semaphore", "tryAcquire")) {
//        // ..., [boolean success]
//        mv.visitInsn(Opcodes.DUP);
//        // ..., [boolean success], [boolean success]
//        // jump if return value is false
//        final Label tryFailed = new Label();
//        mv.visitJumpInsn(Opcodes.IFEQ, tryFailed);
//        // ..., [boolean success]
//        this.pushReceiverForEvent(mv);
//        // ..., [boolean success], objRef
//        this.pushSiteId(mv);
//        // ..., [boolean success], objRef, callSiteId (long)
//        ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.TRY_CALL_SUCCEEDED);
//        // ..., [boolean success]
//        
//        mv.visitLabel(tryFailed);
//      }
//    } catch (final ClassNotFoundException e) {
//      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
//    }
//  }
  
  private HappensBefore getHappensBefore() {
    return null;
  }
  
  private void instrumentHappensBefore(
      final MethodVisitor mv, final Configuration config) {
    // XXX: make this real
    final HappensBefore hb = getHappensBefore();
    if (hb != null) {
      /* Check the return value of the method call to see if we should
       * generate an event.
       */
      final Label skip = new Label();
      final ReturnCheck check = hb.getReturnCheck();
      if (check != ReturnCheck.NONE) {
        // ..., [return value]
        mv.visitInsn(Opcodes.DUP);
        // ..., [return value], [return value]
        mv.visitJumpInsn(check.getOpcode(), skip);
        // ..., [return value]
      }
      
      // ...
      hb.insertInstrumentation(mv, config, this);
      mv.visitLabel(skip);
    }
  }
}
