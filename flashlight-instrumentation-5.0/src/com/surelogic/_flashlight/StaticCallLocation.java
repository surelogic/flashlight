package com.surelogic._flashlight;

public class StaticCallLocation extends AbstractCallLocation {
	private final long f_withinClassId;
	private final int f_line;
	private final String f_memberName;
	private final String f_fileName;
	
	public final long getWithinClassId() {
		return f_withinClassId;
	}

	public final int getLine() {
		return f_line;
	}
	
	public String getLocationName() {
		return f_memberName;
	}
	
	public String getFileName() {
		return f_fileName;
	}
	
	StaticCallLocation(long siteId, String memberName, int line, String file, long declaringType) {
		super(siteId);
		f_memberName = memberName;
		f_line = line;
		f_withinClassId = declaringType;
		f_fileName = file;
	}

	@Override
	protected void accept(EventVisitor v) {
		v.visit(this);
	}
	
	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<static-call-location");
		Entities.addAttribute("id", getSiteId(), b);
		Entities.addAttribute("in-class", f_withinClassId, b);
		Entities.addAttribute("line", f_line, b);
		Entities.addAttribute("location", f_memberName, b);
		Entities.addAttribute("file", f_fileName, b);
		b.append("/>");
		return b.toString();
	}
}
