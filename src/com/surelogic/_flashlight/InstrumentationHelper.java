package com.surelogic._flashlight;

import java.lang.reflect.Field;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.FieldSignature;
import org.aspectj.lang.reflect.SourceLocation;

public final class InstrumentationHelper {

	static void beforeTrace(final JoinPoint.StaticPart enclosingStaticPart, 
			                final JoinPoint.StaticPart staticPart) {
		final Signature signature = enclosingStaticPart.getSignature();
		final String locationName = signature.getName();
		final String declaringTypeName = signature.getDeclaringTypeName();
		Store.beforeTrace(declaringTypeName, locationName, makeSrcLoc(staticPart));
	}

	static void beforeIntrinsicLockAcquisition(final Object o,
			final JoinPoint join, final JoinPoint.StaticPart staticPart) {
		final Object oThis = join.getThis();
		final boolean lockIsThis = (oThis == null ? false : oThis == o);
		boolean lockIsClass = false;
		if (!lockIsThis) {
			final SourceLocation sl = staticPart.getSourceLocation();
			final Class oClass = sl.getWithinType();
			if (oClass == o)
				lockIsClass = true;
		}
		Store.beforeIntrinsicLockAcquisition(o, lockIsThis, lockIsClass,
				makeSrcLoc(staticPart));
	}

	static void beforeIntrinsicLockWait(final JoinPoint join,
			final JoinPoint.StaticPart staticPart) {
		Store.beforeIntrinsicLockWait(join.getTarget(), makeSrcLoc(staticPart));
	}

	static void fieldRead(final JoinPoint join, final JoinPoint.StaticPart staticPart) {
		Store.fieldRead(join.getTarget(), getField(staticPart), makeSrcLoc(staticPart));
	}

	private static Field getField(final JoinPoint.StaticPart staticPart) {
		final FieldSignature signature = (FieldSignature) staticPart.getSignature();
		final Field field = signature.getField();
		return field;
	}

	static void fieldWrite(final JoinPoint join, final JoinPoint.StaticPart staticPart) {
		Store.fieldWrite(join.getTarget(), getField(staticPart), makeSrcLoc(staticPart));
	}

	static void afterTrace(final JoinPoint.StaticPart staticPart) {
		Store.afterTrace(makeSrcLoc(staticPart));
	}

	static void afterIntrinsicLockWait(final JoinPoint join,
			final JoinPoint.StaticPart staticPart) {
		final Object oThis = join.getTarget();
		Store.afterIntrinsicLockWait(oThis, makeSrcLoc(staticPart));
	}

	static void afterIntrinsicLockAcquisition(final Object o,
			final JoinPoint.StaticPart staticPart) {
		Store.afterIntrinsicLockAcquisition(o, makeSrcLoc(staticPart));
	}

	static void afterIntrinsicLockRelease(final Object o,
			final JoinPoint.StaticPart staticPart) {
		Store.afterIntrinsicLockRelease(o, makeSrcLoc(staticPart));
	}

	private static SrcLoc makeSrcLoc(final JoinPoint.StaticPart staticPart) {
		final SourceLocation sl = staticPart.getSourceLocation();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		return location;
	}

	private InstrumentationHelper() {
		// no instances
	}
}
