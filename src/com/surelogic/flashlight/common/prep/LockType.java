package com.surelogic.flashlight.common.prep;

public enum LockType {
	INTRINSIC("I"), UTIL("U");

	private final String flag;

	LockType(String flag) {
		this.flag = flag;
	}

	public String getFlag() {
		return flag;
	}
}
