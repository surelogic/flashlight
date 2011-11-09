package com.surelogic._flashlight.common;

public enum CollectionType {
	ONLY_LOCKS(false) {
		// FIX to turn on locks
	},
	ALL(true);
	
	private boolean defaultValue;

	private CollectionType(boolean defValue) {
		defaultValue = defValue;
	}
	
	public boolean processFieldAccesses() {
		return defaultValue;
	}
	
	public boolean processIndirectAccesses() {
		return defaultValue;
	}
	
	public static CollectionType valueOf(String name, CollectionType defValue) {
		if (name != null) {
			for(CollectionType val : values()) {
				if (val.toString().equals(name)) {
					return val;
				}
			}
		}
		return defValue;
	}
}
