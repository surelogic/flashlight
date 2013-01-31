package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.FlagType.GOT_LOCK;

import com.surelogic._flashlight.common.PreppedAttributes;

public final class AfterUtilConcurrentLockAcquisitionAttempt extends Lock {

	public AfterUtilConcurrentLockAcquisitionAttempt(
			final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	@Override
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

	@Override
	protected Boolean isSuccess(PreppedAttributes attr) {
		return attr.getBoolean(GOT_LOCK);
	}
}
