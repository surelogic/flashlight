package com.surelogic.flashlight.common.prep;

public final class BeforeIntrinsicLockAcquisition extends Lock {

	public BeforeIntrinsicLockAcquisition(
			final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	@Override
  public String getXMLElementName() {
		return "before-intrinsic-lock-acquisition";
	}

	@Override
	protected LockState getState() {
		return LockState.BEFORE_ACQUISITION;
	}

	@Override
	protected LockType getType() {
		return LockType.INTRINSIC;
	}
}
