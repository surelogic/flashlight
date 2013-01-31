package com.surelogic.flashlight.common.prep;


public final class AfterIntrinsicLockRelease extends Lock {

	public AfterIntrinsicLockRelease(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	@Override
  public String getXMLElementName() {
		return "after-intrinsic-lock-release";
	}

	@Override
	protected LockState getState() {
		return LockState.AFTER_RELEASE;
	}

	@Override
	protected LockType getType() {
		return LockType.INTRINSIC;
	}

}
