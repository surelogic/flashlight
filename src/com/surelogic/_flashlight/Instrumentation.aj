package com.surelogic._flashlight;

import java.lang.reflect.Field;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.*;

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
		InstrumentationHelper.beforeTrace(thisEnclosingJoinPointStaticPart
				.getSignature(), thisJoinPointStaticPart.getSourceLocation());
	}

	before(Object o) : intrinsicLock(o) {
		InstrumentationHelper.beforeIntrinsicLockAcquisition(o, thisJoinPoint
				.getThis(), thisJoinPointStaticPart.getSourceLocation());
	}

	before() : intrinsicWait() {
		InstrumentationHelper.beforeIntrinsicLockWait(
				thisJoinPoint.getTarget(), thisJoinPointStaticPart
						.getSourceLocation());
	}

	after() : getField() {
		InstrumentationHelper.fieldRead(
				(FieldSignature) thisJoinPointStaticPart.getSignature(),
				thisJoinPoint.getTarget(), thisJoinPointStaticPart
						.getSourceLocation());
	}

	after() : setField() {
		InstrumentationHelper.fieldWrite(
				(FieldSignature) thisJoinPointStaticPart.getSignature(),
				thisJoinPoint.getTarget(), thisJoinPointStaticPart
						.getSourceLocation());
	}

	after() : trace() {
		InstrumentationHelper.afterTrace(thisJoinPointStaticPart
				.getSourceLocation());
	}

	after() : intrinsicWait() {
		InstrumentationHelper.afterIntrinsicLockWait(thisJoinPoint.getTarget(),
				thisJoinPointStaticPart.getSourceLocation());
	}

	after(Object o) : intrinsicLock(o) {
		InstrumentationHelper.afterIntrinsicLockAcquisition(o,
				thisJoinPointStaticPart.getSourceLocation());
	}

	after(Object o) : intrinsicUnlock(o) {
		InstrumentationHelper.afterIntrinsicLockRelease(o,
				thisJoinPointStaticPart.getSourceLocation());
	}
}
