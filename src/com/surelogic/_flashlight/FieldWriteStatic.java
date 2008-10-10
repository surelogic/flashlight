package com.surelogic._flashlight;

final class FieldWriteStatic extends FieldAccessStatic {

	FieldWriteStatic(final long field, final long siteId, Store.State state) {
		super(field, siteId, state);
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<field-write");
		addNanoTime(b);
		addThread(b);
		addField(b);
		b.append("/>");
		return b.toString();
	}
}
