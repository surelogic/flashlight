package com.surelogic._flashlight;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.FieldSignature;
import org.aspectj.lang.reflect.SourceLocation;

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

	pointcut constructorExecution() :
		execution( *.new(..)) && nofl();

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

	pointcut ucLock() : (call(public void java.util.concurrent.locks.Lock.lock()) || call(public void java.util.concurrent.locks.Lock.lockInterruptibly())) && nofl();

	pointcut ucTryLock() : call(public boolean java.util.concurrent.locks.Lock.tryLock(..)) && nofl();

	pointcut ucUnlock() : call(public void java.util.concurrent.locks.Lock.unlock()) && nofl();

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
		constructorCallHelper(true, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	before() : constructorExecution() {
		constructorExecutionHelper(true, thisJoinPoint, thisJoinPointStaticPart);
	}

	before() : method() {
		methodCallHelper(true, thisJoinPoint, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	before(Object o) : intrinsicLock(o) {
		beforeIntrinsicLockHelper(o, thisJoinPoint, thisJoinPointStaticPart);
	}

	before() : intrinsicWait() {
		intrinsicWaitHelper(true, thisJoinPoint, thisJoinPointStaticPart);
	}

	before() : ucLock()  || ucTryLock() {
		beforeUCLockAcquisitionAttemptHelper(thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after() : getField() {
		fieldAccessHelper(true, thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : setField() {
		fieldAccessHelper(false, thisJoinPoint, thisJoinPointStaticPart);
	}

	after() : constructor() {
		constructorCallHelper(false, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	after() : constructorExecution() {
		constructorExecutionHelper(false, thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after() : method() {
		methodCallHelper(false, thisJoinPoint, thisJoinPointStaticPart,
				thisEnclosingJoinPointStaticPart);
	}

	after(Object o) : intrinsicLock(o) {
		afterIntrinisicLockHelper(true, o, thisJoinPointStaticPart);
	}

	after() : intrinsicWait() {
		intrinsicWaitHelper(false, thisJoinPoint, thisJoinPointStaticPart);
	}

	after(Object o) : intrinsicUnlock(o) {
		afterIntrinisicLockHelper(false, o, thisJoinPointStaticPart);
	}

	after() returning : ucLock() {
		afterUCLockAcquisitionAttemptHelper(true, thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after() throwing : ucLock() {
		afterUCLockAcquisitionAttemptHelper(false, thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after() returning(boolean gotTheLock) : ucTryLock() {
		afterUCLockAcquisitionAttemptHelper(gotTheLock, thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after() throwing : ucTryLock() {
		afterUCLockAcquisitionAttemptHelper(false, thisJoinPoint,
				thisJoinPointStaticPart);
	}

	after() returning : ucUnlock() {
		afterUCLockReleaseHelper(true, thisJoinPoint, thisJoinPointStaticPart);
	}

	after() throwing : ucUnlock() {
		afterUCLockReleaseHelper(false, thisJoinPoint, thisJoinPointStaticPart);
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
		Store.fieldAccess(read, receiver, field, sl.getWithinType(), sl
				.getLine());
	}

	void constructorCallHelper(final boolean before,
			final JoinPoint.StaticPart jpsp,
			final JoinPoint.StaticPart enclosing) {
		final Signature enclosingSignature = enclosing.getSignature();
		final ConstructorSignature constructorSignature = (ConstructorSignature) jpsp
				.getSignature();
		final Constructor constructor = constructorSignature.getConstructor();
		final SourceLocation sl = jpsp.getSourceLocation();
		final String enclosingLocationName = enclosingSignature.getName();
		final String enclosingFileName = sl.getFileName();
		Store.constructorCall(before, constructor, enclosingFileName,
				enclosingLocationName, sl.getWithinType(), sl.getLine());
	}

	void constructorExecutionHelper(final boolean before, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp) {
		final Object receiver = jp.getTarget();
		final SourceLocation sl = jpsp.getSourceLocation();
		Store.constructorExecution(before, receiver, sl.getWithinType(), sl
				.getLine());
	}

	void methodCallHelper(final boolean before, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp,
			final JoinPoint.StaticPart enclosing) {
		final Signature enclosingSignature = enclosing.getSignature();
		final Object receiver = jp.getTarget();
		final SourceLocation sl = jpsp.getSourceLocation();
		final String enclosingLocationName = enclosingSignature.getName();
		final String enclosingFileName = sl.getFileName();
		Store.methodCall(before, receiver, enclosingFileName,
				enclosingLocationName, sl.getWithinType(), sl.getLine());
	}

	void beforeIntrinsicLockHelper(final Object o, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp) {
		final Object oThis = jp.getThis();
		final boolean lockIsThis = (oThis == null ? false : oThis == o);
		final SourceLocation sl = jpsp.getSourceLocation();
		boolean lockIsClass = false;
		if (!lockIsThis) {
			final Class oClass = sl.getWithinType();
			if (oClass == o)
				lockIsClass = true;
		}
		Store.beforeIntrinsicLockAcquisition(o, lockIsThis, lockIsClass, sl
				.getWithinType(), sl.getLine());
	}

	void afterIntrinisicLockHelper(final boolean lockAcquisition,
			final Object o, final JoinPoint.StaticPart jpsp) {
		final SourceLocation sl = jpsp.getSourceLocation();
		if (lockAcquisition)
			Store.afterIntrinsicLockAcquisition(o, sl.getWithinType(), sl
					.getLine());
		else
			Store
					.afterIntrinsicLockRelease(o, sl.getWithinType(), sl
							.getLine());
	}

	void intrinsicWaitHelper(final boolean before, final JoinPoint jp,
			final JoinPoint.StaticPart jpsp) {
		final Object oThis = jp.getThis();
		final SourceLocation sl = jpsp.getSourceLocation();
		Store
				.intrinsicLockWait(before, oThis, sl.getWithinType(), sl
						.getLine());
	}

	void beforeUCLockAcquisitionAttemptHelper(final JoinPoint jp,
			final JoinPoint.StaticPart jpsp) {
		final Object oThis = jp.getThis();
		final SourceLocation sl = jpsp.getSourceLocation();
		Store.beforeUCLockAcquisitionAttempt(oThis, sl.getWithinType(), sl
				.getLine());
	}

	void afterUCLockAcquisitionAttemptHelper(final boolean gotTheLock,
			final JoinPoint jp, final JoinPoint.StaticPart jpsp) {
		final Object oThis = jp.getThis();
		final SourceLocation sl = jpsp.getSourceLocation();
		Store.afterUCLockAcquisitionAttempt(gotTheLock, oThis, sl
				.getWithinType(), sl.getLine());
	}

	void afterUCLockReleaseHelper(final boolean releasedTheLock,
			final JoinPoint jp, final JoinPoint.StaticPart jpsp) {
		final Object oThis = jp.getThis();
		final SourceLocation sl = jpsp.getSourceLocation();
		Store.afterUCLockRelease(releasedTheLock, oThis, sl.getWithinType(), sl
				.getLine());
	}
}
