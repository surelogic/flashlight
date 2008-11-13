package com.surelogic._flashlight;

enum OutputType {
	FL(false, false), 
	FL_GZ(false, true), 
	FLB(true, false), 
	FLB_GZ(true, true);
	
	private final boolean binary, compressed;
	
	OutputType(boolean bin, boolean gz) {
		binary = bin;
		compressed = gz;
	}
	
	boolean isBinary() {
		return binary;
	}
	boolean isCompressed() {
		return compressed;
	}
	
	static OutputType valueOf(String name, OutputType defValue) {
		if (name != null) {
			for(OutputType val : values()) {
				if (val.toString().equals(name)) {
					return val;
				}
			}
		}
		return defValue;
	}
}
