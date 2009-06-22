package com.surelogic._flashlight.common;

public enum CollectionType {
	ONLY_LOCKS(true) {
		// FIX
		public boolean processFieldAccesses() { return false; }
	},
	ALL(true);
	
	private boolean defaultValue;

	private CollectionType(boolean defValue) {
		defaultValue = defValue;
	}
	
	public boolean processFieldAccesses() {
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
