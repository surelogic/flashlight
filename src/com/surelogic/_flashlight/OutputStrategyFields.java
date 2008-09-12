package com.surelogic._flashlight;

import java.io.*;

public class OutputStrategyFields extends EventVisitor {
	static class Buffer {
		byte[] buf = new byte[64];
		int count;
		
		public void clear() {
			count = 0;
		}

		public void append(byte b) {
			if (count == buf.length) {
				byte[] tmp = new byte[buf.length << 1];
				System.arraycopy(buf, 0, tmp, 0, buf.length);
				buf = tmp;
			}
			buf[count] = b;
			count++;
		}
	
		public void append(char c) {
			byte b0 = (byte) (c & 0xff);
			append(b0);
			byte b1 = (byte) (c >> 8);
			append(b1);
		}
	}
	
	private final OutputStream f_out;
	private final Buffer buf = new Buffer();
	
	public OutputStrategyFields(OutputStream stream) {
		f_out = stream;
	}

	@Override
	void visit(final FieldReadInstance e) {	
		buf.clear();

		// FieldReadInstance
		buf.append('R');
		//buf.append('t');
		write(e.getNanoTime());
		//buf.append('T');
		write(e.getWithinThread().getId());
		//buf.append('C');
		write(e.getLocation().getWithinClassId());
		//buf.append('l');
		write(e.getLocation().getLine());
		//buf.append('F');
		write(e.getField().getId());
		//buf.append('R');
		write(e.getReceiver().getId());
		if (e.receiverUnderConstruction()) {
			buf.append('U');
		}

		try {
			f_out.write(buf.buf, 0, buf.count);
		} catch (IOException e1) {
			e1.printStackTrace();
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
