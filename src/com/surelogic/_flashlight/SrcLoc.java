package com.surelogic._flashlight;

final class SrcLoc {

	static final SrcLoc UNKNOWN = new SrcLoc("unknown", 0);

	private final int f_line;

	int getLine() {
		return f_line;
	}

	private final String f_fileName;

	String getFileName() {
		return f_fileName;
	}

	SrcLoc(final String fileName, final int line) {
		f_fileName = fileName;
		f_line = line;
	}

	@Override
	public String toString() {
		return f_fileName + ":" + f_line;
	}
}
