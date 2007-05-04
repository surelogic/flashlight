package com.surelogic._flashlight;

import java.io.PrintWriter;

final class OutputStrategyXML extends EventVisitor {

	private final PrintWriter f_out;
	private String f_indent = "";

	public OutputStrategyXML(final PrintWriter out) {
		assert out != null;
		f_out = out;
		o("<?xml version='1.0' encoding='" + Store.ENCODING
				+ "' standalone='yes'?>");
		o("<flashlight version='1.0'>");
		f_indent = "  ";
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
	void visit(BeforeIntrinsicLockAcquisition e) {
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
	void visit(ObjectDefinition e) {
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

	private void o(final String s) {
		f_out.print(f_indent);
		f_out.println(s);
	}
}
