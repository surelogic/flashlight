package com.surelogic._flashlight;

final class FieldReadStatic extends FieldAccess {

	FieldReadStatic(final ObservedField field, final SrcLoc location) {
		super(field, location);
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<field-read");
		addNanoTime(b);
		addThread(b);
		addField(b);
		b.append("/>");
		return b.toString();
	}
}
