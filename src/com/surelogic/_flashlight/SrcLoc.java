package com.surelogic._flashlight;

final class SrcLoc {

	static final SrcLoc UNKNOWN = new SrcLoc(UnknownError.class, 0);

	private final int f_line;

	int getLine() {
		return f_line;
	}

	private final ClassPhantomReference f_withinClass;
	
	long getWithinClassId() {
		return f_withinClass.getId();
	}

	SrcLoc(Class<?> withinClass, final int line) {
		if (withinClass == null)
			withinClass = UnknownError.class;
		f_withinClass = Phantom.ofClass(withinClass);
		f_line = line;
	}

	@Override
	public String toString() {
		return f_withinClass + ":" + f_line;
	}
}
