package com.surelogic.flashlight.common.prep;

public final class BeforeIntrinsicLockAcquisition extends IntrinsicLock {

	public String getXMLElementName() {
		return "before-intrinsic-lock-acquisition";
	}

	@Override
	protected IntrinsicLockState getState() {
		return IntrinsicLockState.BEFORE_ACQUISITION;
	}
}
