package com.surelogic._flashlight.rewriter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBefore;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeCollection;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeObject;
import com.surelogic._flashlight.common.HappensBeforeConfig.HappensBeforeSwitch;
import com.surelogic._flashlight.common.HappensBeforeConfig.ReturnCheck;
import com.surelogic._flashlight.rewriter.ClassAndFieldModel.ClassNotFoundException;
import com.surelogic._flashlight.rewriter.HappensBeforeTable.Result;
import com.surelogic._flashlight.rewriter.config.Configuration;

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
  protected final HappensBeforeTable happensBefore;
  
  /**
   * @param opcode The opcode used to invoke the method.
   * @param owner The owner of the method.
   * @param originalName The name of the method.
   * @param originalDesc The descriptor of the method.
   */
  public MethodCall(final RewriteMessenger msg,
      final ClassAndFieldModel model, final HappensBeforeTable hbt,
      final int opcode, final String owner,
      final String originalName, final String originalDesc) {
    this.opcode = opcode;
    this.owner = owner;
    this.name = originalName;
    this.descriptor = originalDesc;
    this.returnType = Type.getReturnType(originalDesc);
    
    messenger = msg;
    classModel = model;
    happensBefore = hbt;
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

    // Non-null if the call is interesting for happens-before events
    Result hbResult;
    try {
      hbResult = happensBefore.getHappensBefore(owner, name, descriptor);
    } catch (final ClassNotFoundException e) {
      hbResult = null;
      messenger.warning("Provided classpath is incomplete: couldn't find class " + e.getMissingClass());
    }

    // Get the current nanoTime for happens before events
    if (hbResult != null) {
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, FlashlightNames.JAVA_LANG_SYSTEM,
          FlashlightNames.NANO_TIME.getName(),
          FlashlightNames.NANO_TIME.getDescriptor());
    }
    /* stack is either
     *   ...
     * or
     *   ..., nanoTime (long)
     */
    
    /* original method call */
    
    this.pushReceiverAndArguments(mv); // +X
    /* stack is either
     *   ..., rcvr, arg1, ...., argN
     * or
     *   ..., nanoTime (long), rcvr, arg1, ..., argN
     */

    mv.visitLabel(beforeOriginalCall);
    this.invokeMethod(mv);
    mv.visitLabel(afterOriginalCall);
    /* stack is either
     *   ..., [returnValue]
     * or
     *   ..., nanoTime (long), [returnValue]
     */

    /* Additional post-call instrumentation */
    instrumentAfterLockAndLockInterruptibly(mv, config, true); // +4
    instrumentAfterTryLockNormal(mv, config); // +4
    instrumentAfterUnlock(mv, config, true); // +4
    instrumentAfterWait(mv, config); // +4
    
    if (hbResult != null) instrumentHappensBefore(mv, config, hbResult);
    /* Stack is now definitely
     *   ..., [returnValue]
     */
    
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
  
  private void instrumentHappensBefore(
      final MethodVisitor mv, final Configuration config, final Result result) {
      final HappensBefore hb = result.hb;
    final int returnValueSize = getReturnType().getSize();

    // ..., nanoTime (long), [returnValue]
      
    /* Check the return value of the method call to see if we should
     * generate an event.
     */
    final Label skip = new Label();
    final ReturnCheck check = hb.getReturnCheck();
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
      
      /* We know there is a return value, because otherwise we wouldn't be
       * trying to check it here.
       */
      // ..., nanoTime (long), return value
      mv.visitInsn(Opcodes.DUP);
      // ..., nanoTime (long), return value, return value
      mv.visitJumpInsn(opCode, skip);
      // ..., nanoTime (long), return value
    }
    
    // ..., nanoTime (long), [return value]
    hb.invokeSwitch( new InstrumentationSwitch(
        this, mv, config, result.isExact, returnValueSize));
    // ..., [return value]
    
    final Label resume = new Label();
    mv.visitJumpInsn(Opcodes.GOTO, resume);
    
    /* Get here when a return check indicated the method call was not
     * interesting for happens-before event.  Need to get rid of the nanoTime
     * from the stack.  We know that the return value is not void
     */
    mv.visitLabel(skip);
    // ..., nanoTime (long), return value
    if (returnValueSize == 2) {
      mv.visitInsn(Opcodes.DUP2_X2);
      // ..., return value (long), nanoTime (long), return value (long)
      mv.visitInsn(Opcodes.POP2);
      // ..., return value (long), nanoTime (long)
      mv.visitInsn(Opcodes.POP2);
      // ..., return value (long)
    } else {
      mv.visitInsn(Opcodes.DUP_X2);
      // ..., return value, nanoTime (long), return value
      mv.visitInsn(Opcodes.POP);
      // ..., return value, nanoTime (long)
      mv.visitInsn(Opcodes.POP2);
      // ..., return value
    }
    
    mv.visitLabel(resume);
    // ..., [return value]
  }
  
  private static final class InstrumentationSwitch implements HappensBeforeSwitch {
    private final MethodCall mcall;
    private final MethodVisitor mv;
    private Configuration config;
    private final boolean isExact; 
    private final int returnValueSize;
    
    
    
    public InstrumentationSwitch(final MethodCall mcall,
        final MethodVisitor mv, final Configuration config,
        final boolean isExact, final int returnValueSize) {
      this.mcall = mcall;
      this.mv = mv;
      this.config = config;
      this.isExact = isExact;
      this.returnValueSize = returnValueSize;
    }
    
    
    
    private void swapNanoTimeAndReturnValue() {
      // ..., nanoTime (long), [return value]

      /* returnValueSize == 0 means void return, so there isn't a return value
       * on the stack at all.
       */
      if (returnValueSize == 1) {
        mv.visitInsn(Opcodes.DUP_X2);
        // ..., returnValue, nanoTime (long), returnValue
        mv.visitInsn(Opcodes.POP);
        // ..., returnValue, nanoTime (long)
      } else if (returnValueSize == 2) {
        mv.visitInsn(Opcodes.DUP2_X2);
        // ..., returnValue (long), nanoTime (long), returnValue (long)
        mv.visitInsn(Opcodes.POP2);
        // ..., returnValue (long), nanoTime (long)
      }
      // ..., [return value], nanoTime (long)
    }

    private void pushTypeNameForDynamicTesting(final HappensBefore hb) {
      if (isExact) {
        mv.visitInsn(Opcodes.ACONST_NULL);
      } else {
        mv.visitLdcInsn(hb.getQualifiedClass());
      }
    }

    
    
    public void caseHappensBefore(final HappensBefore hb) {
      // ..., nanoTime (long), [return value]
      swapNanoTimeAndReturnValue();
      // ..., [return value], nanoTime (long)
      
      mcall.pushReceiverForEvent(mv);
      // ..., [return value], nanoTime (long), threadRef
      mcall.pushSiteId(mv);
      // ..., [return value], nanoTime (long), threadRef, callSiteId (long)
      /* Push null if the call is exact, or the qualified type name if
       * the result is not exact.
       */
      pushTypeNameForDynamicTesting(hb);
      // ..., [return value], nanoTime (long), threadRef, callSideId (long), [type name or null]
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.HAPPENS_BEFORE_THREAD);
      // ..., [return value]
    }

    public void caseHappensBeforeObject(final HappensBeforeObject hb) {
      // ..., nanoTime (long), [return value]
      swapNanoTimeAndReturnValue();
      // ..., [return value], nanoTime (long)
      
      mcall.pushReceiverForEvent(mv);
      // ..., [return value], nanoTime (long), object
      mcall.pushSiteId(mv);
      // ..., [return value], nanoTime (long), object, callSiteId (long)
      /* Push null if the call is exact, or the qualified type name if
       * the result is not exact.
       */
      pushTypeNameForDynamicTesting(hb);
      // ..., [return value], nanoTime (long), object, callSideId (long), [type name or null]
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.HAPPENS_BEFORE_OBJECT);
      // ..., [return value]
    }

    public void caseHappensBeforeCollection(final HappensBeforeCollection hb) {
      // ..., nanoTime (long), [return value]
      
      /* check if the arg pos is 0, if so, then we use the return value,
       * so we have to copy it around the nanoTime value on the stack.
       */
      if (hb.getObjectParam() == 0) {
        if (returnValueSize == 2) {
          /* this really shouldn't ever be the case because we expect the
           * return value to be a object reference for our purposes. But
           * let's generate legal JVM code for this case anyhow. 
           */
          // ..., nanoTime (long), returnValue (long)
          mv.visitInsn(Opcodes.DUP2_X2);
          // ..., returnValue (long), nanoTime (long), returnValue (long)
        } else {
          // ..., nanoTime (long), returnValue
          mv.visitInsn(Opcodes.DUP_X2);
          // ..., returnValue, nanoTime (long), returnValue
        }
      } else {
        // ..., nanoTime (long), [return value]
        swapNanoTimeAndReturnValue();
        // ..., [return value], nanoTime (long)

        /* Otherwise, push the reference, collection, the site id, and
         * the given actual argument
         */
        mcall.pushArgumentForEvent(mv, hb.getObjectParam());
        // ..., [return value], nanoTime (long), item
      }
      // ..., [return value], nanoTime (long), item
      mcall.pushReceiverForEvent(mv);
      // ..., [return value], nanoTime (long), item, collection
      mcall.pushSiteId(mv);
      // ..., [return value], nanoTime (long), item, collectionRef, callSiteId (long)
      /* Push null if the call is exact, or the qualified type name if
       * the result is not exact.
       */
      pushTypeNameForDynamicTesting(hb);
      // ..., [return value], nanoTime (long), item, collection, callSiteId (long), [type name or null]
      ByteCodeUtils.callStoreMethod(mv, config, FlashlightNames.HAPPENS_BEFORE_COLLECTION);
      // ..., [return value]
    }
  }
}
