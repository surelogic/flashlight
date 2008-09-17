package com.surelogic._flashlight;

final class FieldReadStatic extends FieldAccessStatic {

	FieldReadStatic(final ObservedField field, final ClassPhantomReference withinClass, final int line) {
		super(field, withinClass, line);
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
