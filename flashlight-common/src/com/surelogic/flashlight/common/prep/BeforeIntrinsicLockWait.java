package com.surelogic.flashlight.common.prep;

public final class BeforeIntrinsicLockWait extends Lock {

	public BeforeIntrinsicLockWait(final IntrinsicLockDurationRowInserter i) {
		super(i);
	}

	public String getXMLElementName() {
		return "before-intrinsic-lock-wait";
	}

	@Override
	protected LockState getState() {
		return LockState.BEFORE_WAIT;
	}

	@Override
	protected LockType getType() {
		return LockType.INTRINSIC;
	}
}
