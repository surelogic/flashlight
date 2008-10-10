package com.surelogic._flashlight;

abstract class IntrinsicLock extends Lock {
	IntrinsicLock(final Object lockObject, final long siteId, Store.State state) {
		super(lockObject, siteId, state);
	}
}
