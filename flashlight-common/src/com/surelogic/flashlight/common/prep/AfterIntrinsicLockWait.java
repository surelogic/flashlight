package com.surelogic.flashlight.common.prep;

public final class AfterIntrinsicLockWait extends Lock {

	public AfterIntrinsicLockWait(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	@Override
  public String getXMLElementName() {
		return "after-intrinsic-lock-wait";
	}

	@Override
	protected LockState getState() {
		return LockState.AFTER_WAIT;
	}

	@Override
	protected LockType getType() {
		return LockType.INTRINSIC;
	}
}
