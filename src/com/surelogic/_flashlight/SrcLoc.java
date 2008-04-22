package com.surelogic._flashlight;

final class SrcLoc {

	static final SrcLoc UNKNOWN = new SrcLoc(UnknownError.class, 0);

	private final int f_line;

	int getLine() {
		return f_line;
	}

	private final String f_typeName;

	String getTypeName() {
		return f_typeName;
	}

	SrcLoc(Class<?> withinType, final int line) {
		if (withinType == null)
			withinType = UnknownError.class;
		final String typeName = withinType.getName();
		if (typeName != null) {
			f_typeName = typeName;
		} else {
			f_typeName = "<unknown type>";
			Store.logAProblem("No name available for the type " + withinType);
		}
		f_line = line;
	}

	@Override
	public String toString() {
		return f_typeName + ":" + f_line;
	}
}
