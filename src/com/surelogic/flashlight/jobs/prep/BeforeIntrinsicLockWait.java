package com.surelogic.flashlight.jobs.prep;

public final class BeforeIntrinsicLockWait extends IntrinsicLock {

	public String getXMLElementName() {
		return "before-intrinsic-lock-wait";
	}

	@Override
	protected IntrinsicLockState getState() {
		return IntrinsicLockState.BEFORE_WAIT;
	}
}
