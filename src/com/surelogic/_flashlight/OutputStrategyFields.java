package com.surelogic._flashlight;

import java.io.PrintWriter;

public class OutputStrategyFields extends EventVisitor {
	static class Buffer {
		char[] buf = new char[64];
		int count;
		
		public void clear() {
			count = 0;
		}

		public void append(char c) {
			if (count == buf.length) {
				char[] tmp = new char[buf.length << 1];
				System.arraycopy(buf, 0, tmp, 0, buf.length);
				buf = tmp;
			}
			buf[count] = c;
			count++;
		}
		
	}
	
	private final PrintWriter f_out;
	private final Buffer buf = new Buffer();
	
	public OutputStrategyFields(PrintWriter w) {
		f_out = w;
	}

	@Override
	void visit(final FieldReadInstance e) {
		if (false) {
			f_out.println(e.toString());
		} else {		
			buf.clear();
			
			// FieldReadInstance
			buf.append('R');
			buf.append('t');
			write(e.getNanoTime());
			buf.append('T');
			write(e.getWithinThread().getId());
			buf.append('C');
			write(e.getLocation().getWithinClassId());
			buf.append('l');
			write(e.getLocation().getLine());
			buf.append('F');
			write(e.getField().getId());
			buf.append('R');
			write(e.getReceiver().getId());
			if (e.receiverUnderConstruction()) {
				buf.append('U');
			}
			
			f_out.write(buf.buf, 0, buf.count);
		}
	}
	
	void write(char c) {
		buf.append(c);
	}
	
	void write(int i) {
		buf.append((char) (i & 0xffff));
		buf.append((char) (i >> 16));
	}
	
	void write(long l) {
		int i0 = (int) l;
		write(i0);
		int i1 = (int) (l >> 32);
		write(i1);
	}
}
