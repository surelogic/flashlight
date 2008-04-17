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
		final Signature signature = thisEnclosingJoinPointStaticPart
				.getSignature();
		final String locationName = signature.getName();
		final String declaringTypeName = signature.getDeclaringTypeName();
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.beforeTrace(declaringTypeName, locationName, location);
	}

	before(Object o) : intrinsicLock(o) {
		final Object oThis = thisJoinPoint.getThis();
		final boolean lockIsThis = (oThis == null ? false : oThis == o);
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		boolean lockIsClass = false;
		if (!lockIsThis) {
			final Class oClass = sl.getWithinType();
			if (oClass == o)
				lockIsClass = true;
		}
		Store.beforeIntrinsicLockAcquisition(o, lockIsThis, lockIsClass,
				location);
	}

	before() : intrinsicWait() {
		final Object oThis = thisJoinPoint.getTarget();
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.beforeIntrinsicLockWait(oThis, location);
	}

	after() : getField() {
		final FieldSignature signature = (FieldSignature) thisJoinPointStaticPart
				.getSignature();
		final Field field = signature.getField();
		final Object receiver = thisJoinPoint.getTarget();
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.fieldRead(receiver, field, location);
	}

	after() : setField() {
		final FieldSignature signature = (FieldSignature) thisJoinPointStaticPart
				.getSignature();
		final Field field = signature.getField();
		final Object receiver = thisJoinPoint.getTarget();
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.fieldWrite(receiver, field, location);
	}

	after() : trace() {
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterTrace(location);
	}

	after() : intrinsicWait() {
		final Object oThis = thisJoinPoint.getTarget();
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterIntrinsicLockWait(oThis, location);
	}

	after(Object o) : intrinsicLock(o) {
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterIntrinsicLockAcquisition(o, location);
	}

	after(Object o) : intrinsicUnlock(o) {
		final SourceLocation sl = thisJoinPointStaticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterIntrinsicLockRelease(o, location);
	}
}
