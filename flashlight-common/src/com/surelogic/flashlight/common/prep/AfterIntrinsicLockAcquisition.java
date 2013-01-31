package com.surelogic.flashlight.common.prep;


public final class AfterIntrinsicLockAcquisition extends Lock {

	public AfterIntrinsicLockAcquisition(
			final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	@Override
  public String getXMLElementName() {
		return "after-intrinsic-lock-acquisition";
	}

	@Override
	protected LockState getState() {
		return LockState.AFTER_ACQUISITION;
	}

	@Override
	protected LockType getType() {
		return LockType.INTRINSIC;
	}

}
