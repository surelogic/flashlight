package com.surelogic.flashlight.common.prep;

public final class AfterUtilConcurrentLockReleaseAttempt extends Lock {

	public AfterUtilConcurrentLockReleaseAttempt(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
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
