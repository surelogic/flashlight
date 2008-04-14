package com.surelogic.flashlight.common.prep;

public enum IntrinsicLockState {
	BEFORE_ACQUISITION, AFTER_ACQUISITION() {
		@Override boolean isLockHeld() {
			return true;
		}
	}, BEFORE_WAIT, AFTER_WAIT() {
		@Override boolean isLockHeld() {
			return true;
		}
	}, AFTER_RELEASE;
	
	boolean isLockHeld() {
		return false;
	}
}
