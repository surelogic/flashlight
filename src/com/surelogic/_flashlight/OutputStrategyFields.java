package com.surelogic._flashlight;

import java.io.PrintWriter;

public class OutputStrategyFields extends EventVisitor {
	private final PrintWriter f_out;
	
	public OutputStrategyFields(PrintWriter w) {
		f_out = w;
	}

	@Override
	void visit(final FieldReadInstance e) {
		if (true) {
			f_out.println(e.toString());
		} else {
			// FieldReadInstance
			f_out.write('R');
			f_out.write('t');
			write(e.getNanoTime());
			f_out.write('T');
			write(e.getWithinThread().getId());
			f_out.write('C');
			write(e.getLocation().getWithinClassId());
			f_out.write('l');
			write(e.getLocation().getLine());
			f_out.write('F');
			write(e.getField().getId());
			f_out.write('R');
			write(e.getReceiver().getId());
			if (e.receiverUnderConstruction()) {
				f_out.write('U');
			}
		}
	}
	
	void write(int i) {
		f_out.write(i & 0xffff);
		f_out.write(i >> 16);
	}
	
	void write(long l) {
		int i0 = (int) l;
		f_out.write(i0);
		int i1 = (int) (l >> 32);
		f_out.write(i1);
	}
}
