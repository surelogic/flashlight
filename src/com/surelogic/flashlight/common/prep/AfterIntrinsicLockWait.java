package com.surelogic.flashlight.common.prep;

public final class AfterIntrinsicLockWait extends IntrinsicLock {

	public String getXMLElementName() {
		return "after-intrinsic-lock-wait";
	}

	@Override
	protected IntrinsicLockState getState() {
		return IntrinsicLockState.AFTER_WAIT;
	}
}
