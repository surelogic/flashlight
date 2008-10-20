package com.surelogic.flashlight.common.prep;

public final class AfterUtilConcurrentLockAcquisitionAttempt extends Lock {

	public AfterUtilConcurrentLockAcquisitionAttempt(
			final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public String getXMLElementName() {
		return "after-util-concurrent-lock-acquisition-attempt";
	}

	@Override
	protected LockState getState() {
		return LockState.AFTER_ACQUISITION;
	}

	@Override
	protected LockType getType() {
		return LockType.UTIL;
	}
}
