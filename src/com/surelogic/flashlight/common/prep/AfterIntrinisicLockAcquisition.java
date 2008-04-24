package com.surelogic.flashlight.common.prep;

public final class AfterIntrinisicLockAcquisition extends Lock {

	public AfterIntrinisicLockAcquisition(BeforeTrace before) {
		super(before);
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
