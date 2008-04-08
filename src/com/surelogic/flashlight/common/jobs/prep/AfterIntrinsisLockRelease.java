package com.surelogic.flashlight.jobs.prep;

public final class AfterIntrinsisLockRelease extends IntrinsicLock {

	public String getXMLElementName() {
		return "after-intrinsic-lock-release";
	}

	@Override
	protected IntrinsicLockState getState() {
		return IntrinsicLockState.AFTER_RELEASE;
	}
}
