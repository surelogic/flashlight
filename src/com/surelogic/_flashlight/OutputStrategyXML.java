package com.surelogic._flashlight;

import java.io.PrintWriter;

final class OutputStrategyXML extends EventVisitor {

	private final PrintWriter f_out;
	private String f_indent = "";

	private void o(final String s) {
		f_out.print(f_indent);
		f_out.println(s);
	}

	private void addProperty(final String key, final StringBuilder b) {
		String prop = System.getProperty(key);
		if (prop == null)
			prop = "UNKNOWN";
		Entities.addAttribute(key.replaceAll("\\.", "-"), prop, b);
	}

	public OutputStrategyXML(final PrintWriter out) {
		assert out != null;
		f_out = out;
		o("<?xml version='1.0' encoding='" + Store.ENCODING
				+ "' standalone='yes'?>");
		StringBuilder b = new StringBuilder();
		b.append("<flashlight");
		Entities.addAttribute("version", "1.0", b);
		Entities.addAttribute("run", Store.getRun(), b);
		b.append(">"); // don't end this element
		o(b.toString());
		f_indent = "  ";
		b = new StringBuilder();
		b.append("<environment");
		addProperty("user.name", b);
		addProperty("java.version", b);
		addProperty("java.vendor", b);
		addProperty("os.name", b);
		addProperty("os.arch", b);
		addProperty("os.version", b);
		Entities.addAttribute("max-memory-mb", Runtime.getRuntime().maxMemory()
				/ (1024L * 1024L), b);
		Entities.addAttribute("processors", Runtime.getRuntime()
				.availableProcessors(), b);
		b.append("/>");
		o(b.toString());
	}

	@Override
	void visit(AfterIntrinsicLockAcquisition e) {
		o(e.toString());
	}

	@Override
	void visit(AfterIntrinsicLockRelease e) {
		o(e.toString());
	}

	@Override
	void visit(AfterIntrinsicLockWait e) {
		o(e.toString());
	}

	@Override
	void visit(final AfterTrace e) {
		o(e.toString());
	}

	@Override
	void visit(BeforeIntrinsicLockAcquisition e) {
		o(e.toString());
	}

	@Override
	void visit(BeforeIntrinsicLockWait e) {
		o(e.toString());
	}

	@Override
	void visit(BeforeTrace e) {
		o(e.toString());
	}

	@Override
	void visit(FieldDefinition e) {
		o(e.toString());
	}

	@Override
	void visit(FieldReadInstance e) {
		o(e.toString());
	}

	@Override
	void visit(FieldReadStatic e) {
		o(e.toString());
	}

	@Override
	void visit(FieldWriteInstance e) {
		o(e.toString());
	}

	@Override
	void visit(FieldWriteStatic e) {
		o(e.toString());
	}

	@Override
	void visit(FinalEvent e) {
		f_indent = "";
		o("</flashlight>");
		f_out.close();
	}

	@Override
	void visit(GarbageCollectedObject e) {
		o(e.toString());
	}

	@Override
	void visit(ObjectDefinition e) {
		o(e.toString());
	}

	@Override
	void visit(ReadWriteLockDefinition e) {
		o(e.toString());
	}

	@Override
	void visit(SingleThreadedFieldInstance e) {
		o(e.toString());
	}

	@Override
	void visit(SingleThreadedFieldStatic e) {
		o(e.toString());
	}

	@Override
	void visit(Time e) {
		o(e.toString());
	}
}
