package com.surelogic._flashlight;

import java.lang.reflect.Field;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.FieldSignature;
import org.aspectj.lang.reflect.SourceLocation;

public final class InstrumentationHelper {

	static void beforeTrace(final Signature signature, final SourceLocation sl) {
		final String locationName = signature.getName();
		final String declaringTypeName = signature.getDeclaringTypeName();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.beforeTrace(declaringTypeName, locationName, location);
	}

	static void beforeIntrinsicLockAcquisition(final Object o,
			final Object oThis, final SourceLocation sl) {
		final boolean lockIsThis = (oThis == null ? false : oThis == o);
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

	static void beforeIntrinsicLockWait(final Object oThis,
			final SourceLocation sl) {
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.beforeIntrinsicLockWait(oThis, location);
	}

	static void fieldRead(final FieldSignature signature,
			final Object receiver, final SourceLocation sl) {
		final Field field = signature.getField();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.fieldRead(receiver, field, location);
	}

	static void fieldWrite(final FieldSignature signature,
			final Object receiver, final SourceLocation sl) {
		final Field field = signature.getField();
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.fieldWrite(receiver, field, location);
	}

	static void afterTrace(final SourceLocation sl) {
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterTrace(location);
	}

	static void afterIntrinsicLockWait(final Object oThis,
			final SourceLocation sl) {
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterIntrinsicLockWait(oThis, location);
	}

	static void afterIntrinsicLockAcquisition(final Object o,
			final SourceLocation sl) {
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterIntrinsicLockAcquisition(o, location);
	}

	static void afterIntrinsicLockRelease(final Object o,
			final SourceLocation sl) {
		final SrcLoc location = new SrcLoc(sl.getFileName(), sl.getLine());
		Store.afterIntrinsicLockRelease(o, location);
	}

	private InstrumentationHelper() {
		// no instances
	}
}
