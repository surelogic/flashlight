package com.surelogic._flashlight;

final class FieldWriteInstance extends FieldAccessInstance {

	FieldWriteInstance(final Object receiver, final ObservedField field,
			final SrcLoc location) {
		super(receiver, field, location);
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
		addReceiver(b);
		b.append("/>");
		return b.toString();
	}
}
