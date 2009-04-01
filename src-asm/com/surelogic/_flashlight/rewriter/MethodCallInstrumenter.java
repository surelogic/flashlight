package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * Encapsulates the rewriting of a method call to insert instrumentation.
 * Separate because the instrumentation can be generated either by the
 * method visitor when instrumenting method calls made from within interface
 * initializers, or (more typically) from the class visitor when creating
 * wrapper methods.
 */
final class MethodCallInstrumenter {
  private final Configuration config;
  private final MethodVisitor mv;
  private final MethodCall methodCall;
  
  
  public MethodCallInstrumenter(final Configuration config,
      final MethodVisitor mv, final MethodCall methodCall) {
    this.config = config;
    this.mv = mv;
    this.methodCall = methodCall;
  }
  
  
  
  /* In the normal case, needs the stack to be increased by 6.
   * In the exceptional case, needs the stack to be increased by 7.
   * Also, if this is being used to generate a wrapper method, the caller
   * must ensure the stack has enough space to hold the arguments, receiver
   * and return value.
   */
  public void instrumentMethodCall() {
    // ...
    
    /* before method all event */
    instrumentBeforeMethodCall(); // +6
    // ...

    /* Additional pre-call instrumentation */
    instrumentBeforeAnyJUCLockAcquisition(); // +3
    instrumentBeforeWait(); // +4
    // ...

    final Label beforeOriginalCall = new Label();
    final Label afterOriginalCall = new Label();
    final Label exceptionHandler = new Label();
    final Label resume = new Label();
    mv.visitTryCatchBlock(beforeOriginalCall, afterOriginalCall, exceptionHandler, null);
    
    /* original method call */
    methodCall.pushReceiverAndArguments(mv); // +X
    // rcvr, arg1, ..., argN

    mv.visitLabel(beforeOriginalCall);
    methodCall.invokeMethod(mv);
    mv.visitLabel(afterOriginalCall);
    // [returnValue]

    /* Additional post-call instrumentation */
    instrumentAfterLockAndLockInterruptibly(true); // +4
    instrumentAfterTryLockNormal(); // +4
    instrumentAfterUnlock(true); // +4
    instrumentAfterWait(); // +4
    
    /* after method call event */
    instrumentAfterMethodCall(); // +6
    
    /* Jump around exception handler */
    mv.visitJumpInsn(Opcodes.GOTO, resume);

    /* Exception handler: still want to report method exit when there is an exception */
    mv.visitLabel(exceptionHandler);
    // exception

    /* Additional post-call instrumentation */
    instrumentAfterLockAndLockInterruptibly(false); // +4 (+1 exception)
    instrumentAfterTryLockException(); // +4 (+1 exception)
    instrumentAfterUnlock(false); // +4 (+1 exception)
    instrumentAfterWait(); // +4 (+1 exception)
    
    instrumentAfterMethodCall(); // +6 (+1 exception)

    // exception
    mv.visitInsn(Opcodes.ATHROW);

    /* Resume normal execution */
    mv.visitLabel(resume);
  }


  // +4 Stack
  private void instrumentBeforeMethodCall() {
    if (config.instrumentBeforeCall) {
      // empty stack 
      ByteCodeUtils.pushBooleanConstant(mv, true);
      // true
      methodCall.pushReceiverForEvent(mv);
      // true, objRef
      methodCall.pushSiteId(mv);
      // true, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.METHOD_CALL);
      // empty stack 
    }
  }

  // +4 Stack
  private void instrumentAfterMethodCall() {
    if (config.instrumentAfterCall) {
      // ...,
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., false
      methodCall.pushReceiverForEvent(mv);
      // ..., false, objRef
      methodCall.pushSiteId(mv);
      // ..., false, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.METHOD_CALL);
      // ...
    }
  }

  // +4 Stack
  private void instrumentBeforeWait() {
    if (methodCall.testCalledMethodName(
        FlashlightNames.JAVA_LANG_OBJECT, FlashlightNames.WAIT)
        && config.instrumentBeforeWait) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, true);
      // ..., true
      methodCall.pushReceiverForEvent(mv);
      // ..., true, objRef
      methodCall.pushSiteId(mv);
      // ..., true, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INTRINSIC_LOCK_WAIT);      
    }
  }
  
  // +4 Stack
  private void instrumentAfterWait() {
    if (methodCall.testCalledMethodName(
        FlashlightNames.JAVA_LANG_OBJECT, FlashlightNames.WAIT)
        && config.instrumentAfterWait) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., false
      methodCall.pushReceiverForEvent(mv);
      // ..., false, objRef
      methodCall.pushSiteId(mv);
      // ..., false, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.INTRINSIC_LOCK_WAIT);
    }
  }
  
  // +3 Stack
  private void instrumentBeforeAnyJUCLockAcquisition() {
    if ((methodCall.testCalledMethodName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK, FlashlightNames.LOCK)
        || methodCall.testCalledMethodName(
            FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
            FlashlightNames.LOCK_INTERRUPTIBLY)
        || methodCall.testCalledMethodName(
            FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
            FlashlightNames.TRY_LOCK))
        && config.instrumentBeforeJUCLock) {
      // ...
      methodCall.pushReceiverForEvent(mv);
      // ..., objRef
      methodCall.pushSiteId(mv);
      // ..., objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.BEFORE_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);
    }
  }
  
  // +4 Stack
  private void instrumentAfterLockAndLockInterruptibly(final boolean gotTheLock) {
    if ((methodCall.testCalledMethodName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK, FlashlightNames.LOCK)
        || methodCall.testCalledMethodName(
            FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
            FlashlightNames.LOCK_INTERRUPTIBLY))
        && config.instrumentAfterLock) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, gotTheLock);
      // ..., gotTheLock
      methodCall.pushReceiverForEvent(mv);
      // ..., gotTheLock, objRef
      methodCall.pushSiteId(mv);
      // ..., gotTheLock, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);      
    }
  }
  
  // +4 Stack
  private void instrumentAfterTryLockNormal() {
    if (methodCall.testCalledMethodName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
        FlashlightNames.TRY_LOCK) && config.instrumentAfterTryLock) {
      // ..., gotTheLock
      
      /* We need to make a copy of tryLock()'s return value */
      mv.visitInsn(Opcodes.DUP);
      // ..., gotTheLock, gotTheLock      
      methodCall.pushReceiverForEvent(mv);
      // ..., gotTheLock, gotTheLock, objRef
      methodCall.pushSiteId(mv);
      // ..., gotTheLock, gotTheLock, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);
      // ..., gotTheLock
    }
  }
  
  //+4 Stack
  private void instrumentAfterTryLockException() {
    if (methodCall.testCalledMethodName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
        FlashlightNames.TRY_LOCK) && config.instrumentAfterTryLock) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, false);
      // ..., false
      methodCall.pushReceiverForEvent(mv);
      // ..., false, objRef
      methodCall.pushSiteId(mv);
      // ..., false, objRef, callSiteId (false)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_ACQUISITION_ATTEMPT);      
    }
  }
  
  // +4 Stack
  private void instrumentAfterUnlock(final boolean releasedTheLock) {
    if (methodCall.testCalledMethodName(
        FlashlightNames.JAVA_UTIL_CONCURRENT_LOCKS_LOCK,
        FlashlightNames.UNLOCK) && config.instrumentAfterUnlock) {
      // ...
      ByteCodeUtils.pushBooleanConstant(mv, releasedTheLock);
      // ..., gotTheLock
      methodCall.pushReceiverForEvent(mv);
      // ..., gotTheLock, objRef
      methodCall.pushSiteId(mv);
      // ..., gotTheLock, objRef, callSiteId (long)
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.AFTER_UTIL_CONCURRENT_LOCK_RELEASE_ATTEMPT);
    }
  }  
}
