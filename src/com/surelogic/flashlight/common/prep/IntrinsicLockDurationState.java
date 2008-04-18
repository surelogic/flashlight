package com.surelogic.flashlight.common.prep;

public enum IntrinsicLockDurationState {
	IDLE() {
		@Override public boolean isRunning() {
			return true;
		}
	}, 
	BLOCKING, 
	HOLDING() {
		@Override public boolean isRunning() {
			return true;
		}
	}, 
	WAITING;

	public boolean isRunning() {
		return false;
	}
}
