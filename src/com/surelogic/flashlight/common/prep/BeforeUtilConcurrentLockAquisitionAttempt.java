package com.surelogic.flashlight.common.prep;

public class BeforeUtilConcurrentLockAquisitionAttempt extends Lock {

	public BeforeUtilConcurrentLockAquisitionAttempt(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
	}

	public String getXMLElementName() {
		return "before-util-concurrent-lock-acquisition-attempt";
	}

	@Override
	protected LockState getState() {
		return LockState.BEFORE_ACQUISITION;
	}

	@Override
	protected LockType getType() {
		return LockType.UTIL;
	}

}
