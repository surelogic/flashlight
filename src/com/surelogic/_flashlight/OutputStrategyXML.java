package com.surelogic._flashlight;

import java.io.*;

import com.surelogic._flashlight.trace.TraceNode;

final class OutputStrategyXML extends EventVisitor {
	static final String version = "1.0";
	private final PrintWriter f_out;
	private String f_indent = "";
	
	private void o(final String s) {
		f_out.print(f_indent);
		f_out.println(s);
	}

	private static void addProperty(final String key, final StringBuilder b) {
		String prop = System.getProperty(key);
		if (prop == null)
			prop = "UNKNOWN";
		Entities.addAttribute(key.replaceAll("\\.", "-"), prop, b);
	}

	public static void outputHeader(final PrintWriter out, Time time, String version) {
		assert out != null;
		out.println("<?xml version='1.0' encoding='" + Store.ENCODING
				+ "' standalone='yes'?>");
		StringBuilder b = new StringBuilder();
		b.append("<flashlight");
		Entities.addAttribute("version", version, b);
		Entities.addAttribute("run", Store.getRun(), b);
		b.append(">"); // don't end this element
		out.println(b.toString());
		b = new StringBuilder();
		b.append("  <environment");
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
		out.println(b.toString());
		if (time != null) {
			out.print("  ");
			out.println(time);
			out.println("</flashlight>");
		}
	}
	
	static final Factory factory = new Factory() {
		public EventVisitor create(OutputStream stream, String encoding, Time time) throws IOException {
			return new OutputStrategyXML(stream, encoding);
		}
	};
	
	OutputStrategyXML(final OutputStream stream, String encoding) throws IOException {
		assert stream != null;
		
		final OutputStreamWriter osw = new OutputStreamWriter(stream, encoding);
		f_out = new PrintWriter(osw);
		outputHeader(f_out, null, version);
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
	void visit(AfterUtilConcurrentLockAcquisitionAttempt e) {
		o(e.toString());
	}

	@Override
	void visit(AfterUtilConcurrentLockReleaseAttempt e) {
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
	void visit(BeforeUtilConcurrentLockAcquisitionAttempt e) {
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
		//System.out.println("Closed.");
		f_out.close();
		//new Throwable("Visiting FinalEvent").printStackTrace(System.out);
	}

	@Override
	void visit(GarbageCollectedObject e) {
		o(e.toString());
	}

	@Override
	void visit(IndirectAccess e) {
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
	void visit(final SelectedPackage e) {
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
	void visit(final StaticCallLocation e) {
		o(e.toString());
	}
	
	@Override
	void visit(Time e) {
		o(e.toString());
	}
	
	@Override
	public void visit(TraceNode e) {
		o(e.toString());
	}
	
	@Override
	void flush() {
		//System.out.println("Flushed.");
		f_out.flush();
	}
}
