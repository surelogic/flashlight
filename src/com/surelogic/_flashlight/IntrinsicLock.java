package com.surelogic._flashlight;

abstract class IntrinsicLock extends Lock {
	IntrinsicLock(final Object lockObject, final long siteId) {
		super(lockObject, siteId);
	}
}
