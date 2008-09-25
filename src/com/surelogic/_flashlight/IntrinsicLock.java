package com.surelogic._flashlight;

abstract class IntrinsicLock extends Lock {
	IntrinsicLock(final Object lockObject, final ClassPhantomReference withinClass, final int line) {
		super(lockObject, withinClass, line);
	}
}
