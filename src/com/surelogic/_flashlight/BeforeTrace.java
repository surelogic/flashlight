package com.surelogic._flashlight;

final class BeforeTrace extends Trace {

	private final String f_fileName;

	String getDeclartingTypeName() {
		return f_fileName;
	}

	private final String f_locationName;

	String getLocationName() {
		return f_locationName;
	}

	BeforeTrace(final String fileName, final String locationName,
			final SrcLoc location) {
		super(location);
		f_fileName = fileName == null ? "<unknown file name>" : fileName;
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
		Entities.addAttribute("location", f_locationName, b);
		Entities.addAttribute("file", f_fileName, b);
		b.append("/>");
		return b.toString();
	}
}
