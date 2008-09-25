package com.surelogic._flashlight;

public abstract class UtilConcurrentLock extends Lock {
	UtilConcurrentLock(final java.util.concurrent.locks.Lock lockObject, 
			           final ClassPhantomReference withinClass, final int line) {
		super(lockObject, withinClass, line);
	}
}
