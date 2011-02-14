package com.surelogic._flashlight;

abstract class IntrinsicLock extends Lock {
	IntrinsicLock(final Object lockObject, final long siteId,
			final PostMortemStore.State state) {
		super(lockObject, siteId, state);
	}
}
