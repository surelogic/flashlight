package com.surelogic.flashlight.common.prep;

public final class AfterIntrinsicLockAcquisition extends Lock {

	public AfterIntrinsicLockAcquisition(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
	}

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
