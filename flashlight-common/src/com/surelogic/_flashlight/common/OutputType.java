package com.surelogic._flashlight.common;

public enum OutputType {
	FL(false, false), 
	FL_GZ(false, true), 
	FLB(true, false), 
	FLB_GZ(true, true);
	
	private final boolean binary, compressed;
	
	private OutputType(boolean bin, boolean gz) {
		binary = bin;
		compressed = gz;
	}
	
	public boolean isBinary() {
		return binary;
	}
	public boolean isCompressed() {
		return compressed;
	}
	
	public static OutputType valueOf(String name, OutputType defValue) {
		if (name != null) {
			for(OutputType val : values()) {
				if (val.toString().equals(name)) {
					return val;
				}
			}
		}
		return defValue;
	}

	public static OutputType get(String useBinary, boolean compress) {
		boolean binary = "true".equals(useBinary);
		for(OutputType val : values()) {
			if (val.isBinary() == binary && val.isCompressed() == compress) {
				return val;
			}
		}
		return FL_GZ;
	}
}
