package com.surelogic.flashlight.common.prep;

public class AfterUtilConcurrentLockReleaseAttempt extends Lock {

	public AfterUtilConcurrentLockReleaseAttempt(BeforeTrace before) {
		super(before);
	}

	@Override
	protected LockType getType() {
		return LockType.UTIL;
	}

	public String getXMLElementName() {
		return "after-util-concurrent-lock-release-attempt";
	}

	@Override
	protected LockState getState() {
		return LockState.AFTER_RELEASE;
	}

}
