package com.surelogic._flashlight;

public aspect Instrumentation {

	/**
	 * Omits the flashlight instrumentation code.
	 */
	pointcut nofl() : !within(com.surelogic._flashlight..*);

	/**
	 * Instrument field reads and writes.
	 */
	pointcut getField() : get(* *) && nofl();

	pointcut setField() : set(* *) && nofl();

	/**
	 * Instrument traces within the code.
	 */
	pointcut trace() : 
         (call(* *.*(..)) || call( *.new(..))) && nofl();

	/**
	 * Instrument intrinsic lock acquisition. This is done in two steps: before
	 * the lock is acquired and after the lock is acquired. Instrumenting these
	 * two steps separately supports runtime deadlock detection by flashlight.
	 */
	pointcut intrinsicLock(Object o) : lock() && args(o) && nofl();

	/**
	 * Instrument waiting on an intrinsic lock. See the Java Language
	 * Specification (3rd edition) section 17.8 <i>Wait Sets and Notification</i>
	 * for the semantics of waiting on an intrinsic lock.
	 */
	pointcut intrinsicWait() : (
			call(public void wait()) ||
	        call(public void wait(long)) ||
			call(public void wait(long, int))
			) && nofl();

	/**
	 * Instrument intrinsic lock release.
	 */
	pointcut intrinsicUnlock(Object o) : unlock() && args(o) && nofl();

	/*
	 * Advice
	 * 
	 * We need to put 'before' advice before 'after' advice so we don't get a
	 * precedence error from the AspectJ compiler.
	 */

	before() : trace() {
		InstrumentationHelper.beforeTrace(thisEnclosingJoinPointStaticPart, 
				                          thisJoinPointStaticPart);
	}

	before(Object o) : intrinsicLock(o) {
		InstrumentationHelper.beforeIntrinsicLockAcquisition(o, thisJoinPoint, 
				thisJoinPointStaticPart);
	}

	before() : intrinsicWait() {
		InstrumentationHelper.beforeIntrinsicLockWait(
				thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : getField() {
		InstrumentationHelper.fieldRead(
				thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : setField() {
		InstrumentationHelper.fieldWrite(
				thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : trace() {
		InstrumentationHelper.afterTrace(thisJoinPointStaticPart);
	}

	after() : intrinsicWait() {
		InstrumentationHelper.afterIntrinsicLockWait(thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after(Object o) : intrinsicLock(o) {
		InstrumentationHelper.afterIntrinsicLockAcquisition(o,
				thisJoinPointStaticPart);
	}

	after(Object o) : intrinsicUnlock(o) {
		InstrumentationHelper.afterIntrinsicLockRelease(o,
				thisJoinPointStaticPart);
	}
}
