package com.surelogic.flashlight.jobs.prep;

public final class AfterIntrinisicLockAcquisition extends IntrinsicLock {

	public String getXMLElementName() {
		return "after-intrinsic-lock-acquisition";
	}

	@Override
	protected IntrinsicLockState getState() {
		return IntrinsicLockState.AFTER_ACQUISITION;
	}
}
