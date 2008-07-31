package com.surelogic.flashlight.common.prep;

public final class BeforeIntrinsicLockAcquisition extends Lock {

	public BeforeIntrinsicLockAcquisition(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
	}

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
