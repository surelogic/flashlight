package com.surelogic._flashlight;

public abstract class UtilConcurrentLock extends Lock {
	UtilConcurrentLock(final java.util.concurrent.locks.Lock lockObject, 
			           final long siteId, Store.State state) {
		super(lockObject, siteId, state);
	}
}
