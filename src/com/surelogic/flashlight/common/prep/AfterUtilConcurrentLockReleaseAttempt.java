package com.surelogic.flashlight.common.prep;

public final class AfterUtilConcurrentLockReleaseAttempt extends Lock {

	public AfterUtilConcurrentLockReleaseAttempt(
			final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	@Override
	protected LockType getType() {
		return LockType.UTIL;
	}

	public String getXMLElementName() {
		return "after-util-concurrent-lock-release-attempt";
	}

	@Override
	protected LockState getState() {
		return LockState.AFTER_RELEASE;
	}

}
