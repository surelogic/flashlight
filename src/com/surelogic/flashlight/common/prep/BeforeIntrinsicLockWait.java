package com.surelogic.flashlight.common.prep;

public final class BeforeIntrinsicLockWait extends Lock {

	public BeforeIntrinsicLockWait(final BeforeTrace before,
			final IntrinsicLockDurationRowInserter i) {
		super(before, i);
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
