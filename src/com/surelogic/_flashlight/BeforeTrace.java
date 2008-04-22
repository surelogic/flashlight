package com.surelogic._flashlight;

final class BeforeTrace extends Trace {

	private final String f_declaringTypeName;

	String getDeclartingTypeName() {
		return f_declaringTypeName;
	}

	private final String f_locationName;

	String getLocationName() {
		return f_locationName;
	}

	String getTraceLine() {
		return f_declaringTypeName + "." + f_locationName + "(" + getLocation()
				+ ")";
	}

	BeforeTrace(final String declaringTypeName, final String locationName,
			final SrcLoc location) {
		super(location);
		f_declaringTypeName = declaringTypeName == null ? "<unknown declaring type>"
				: declaringTypeName;
		f_locationName = locationName == null ? "<unknown location>"
				: locationName;
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("<before-trace");
		addNanoTime(b);
		addThread(b);
		Entities.addAttribute("at", getTraceLine(), b);
		b.append("/>");
		return b.toString();
	}
}
