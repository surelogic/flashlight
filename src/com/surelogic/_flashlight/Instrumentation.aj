package com.surelogic._flashlight;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aspectj.lang.*;
import org.aspectj.lang.reflect.*;

public aspect Instrumentation {

	/**
	 * Omits the flashlight instrumentation code.
	 */
	pointcut nofl() : !within(com.surelogic._flashlight..*);

	/**
	 * Instrument field reads.
	 */
	pointcut getField() : get(* *) && nofl();

	/**
	 * Instrument field writes.
	 */
	pointcut setField() : set(* *) && nofl();

	/**
	 * Instrument method calls.
	 */
	pointcut method() : 
         call(* *.*(..)) && nofl();

	/**
	 * Instrument constructor calls.
	 */
	pointcut constructor() : 
		 call( *.new(..)) && nofl();

	/**
	 * Instrument intrinsic lock acquisition. This is done in two steps: before
	 * the lock is acquired and after the lock is acquired. Instrumenting these
	 * two steps separately supports runtime deadlock detection by flashlight.
	 */
	pointcut intrinsicLock(Object o) : lock() && args(o) && nofl();

	/**
	 * Instrument intrinsic lock release.
	 */
	pointcut intrinsicUnlock(Object o) : unlock() && args(o) && nofl();

	/*
	 * Advice
	 * 
	 * We need to put 'before' advice before 'after' advice so we don't get a
	 * precedence error from the AspectJ compiler.
	 * 
	 * It is a good idea to create a helper method to call into the Flashlight
	 * store.
	 */

	before() : constructor() {
		constructorCallHelper(true, thisJoinPoint, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	before() : method() {
		methodCallHelper(true, thisJoinPoint, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	before(Object o) : intrinsicLock(o) {
		beforeIntrinsicLockHelper(o, thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : getField() {
		fieldAccessHelper(true, thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : setField() {
		fieldAccessHelper(false, thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : constructor() {
		constructorCallHelper(false, thisJoinPoint, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	after() : method() {
		methodCallHelper(false, thisJoinPoint, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	after(Object o) : intrinsicLock(o) {
		afterIntrinisicLockHelper(true, o, thisJoinPointStaticPart);
	}

	after(Object o) : intrinsicUnlock(o) {
		afterIntrinisicLockHelper(false, o, thisJoinPointStaticPart);
	}

	/*
	 * Helper methods that call into the Flashlight store.
	 */

	void fieldAccessHelper(final boolean read, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp) {
		final FieldSignature signature = (FieldSignature) jpsp.getSignature();
		final Object receiver = jp.getTarget();
		final SourceLocation sl = jpsp.getSourceLocation();
		final Field field = signature.getField();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.fieldAccess(read, receiver, field, location);
	}

	void constructorCallHelper(final boolean before, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp,
			final JoinPoint.StaticPart enclosing) {
		final Signature enclosingSignature = enclosing.getSignature();
		final ConstructorSignature constructorSignature = (ConstructorSignature) jpsp
				.getSignature();
		final Constructor constructor = constructorSignature.getConstructor();
		final Object receiver = jp.getTarget();
		final SourceLocation sl = jpsp.getSourceLocation();
		final String enclosingLocationName = enclosingSignature.getName();
		final String enclosingDeclaringTypeName = enclosingSignature
				.getDeclaringTypeName();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.constructorCall(before, constructor, receiver,
				enclosingDeclaringTypeName, enclosingLocationName, location);
	}

	void methodCallHelper(final boolean before, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp,
			final JoinPoint.StaticPart enclosing) {
		final Signature enclosingSignature = enclosing.getSignature();
		final MethodSignature methodSignature = (MethodSignature) jpsp
				.getSignature();
		final Method method = methodSignature.getMethod();
		final Object receiver = jp.getTarget();
		final SourceLocation sl = jpsp.getSourceLocation();
		final String enclosingLocationName = enclosingSignature.getName();
		final String enclosingDeclaringTypeName = enclosingSignature
				.getDeclaringTypeName();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.methodCall(before, method, receiver, enclosingDeclaringTypeName,
				enclosingLocationName, location);
	}

	void beforeIntrinsicLockHelper(final Object o, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp) {
		final Object oThis = jp.getThis();
		final boolean lockIsThis = (oThis == null ? false : oThis == o);
		final SourceLocation sl = jpsp.getSourceLocation();
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

	void afterIntrinisicLockHelper(final boolean lockAcquisition,
			final Object o, final JoinPoint.StaticPart jpsp) {
		final SourceLocation sl = jpsp.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		if (lockAcquisition)
			Store.afterIntrinsicLockAcquisition(o, location);
		else
			Store.afterIntrinsicLockRelease(o, location);
	}
}
