package com.surelogic.flashlight.common.prep;

public final class AfterIntrinsisLockRelease extends IntrinsicLock {

	public AfterIntrinsisLockRelease(BeforeTrace before) {
		super(before);
	}

	public String getXMLElementName() {
		return "after-intrinsic-lock-release";
	}

	@Override
	protected IntrinsicLockState getState() {
		return IntrinsicLockState.AFTER_RELEASE;
	}
}
