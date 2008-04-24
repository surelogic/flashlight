package com.surelogic.flashlight.common.prep;

public final class AfterIntrinsisLockRelease extends Lock {

	public AfterIntrinsisLockRelease(BeforeTrace before) {
		super(before);
	}

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
