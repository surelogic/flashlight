package com.surelogic._flashlight;

final class SingleThreadedFieldStatic extends SingleThreadedField {

	SingleThreadedFieldStatic(final ObservedField field) {
		super(field);
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<single-threaded-field");
		addField(b);
		b.append("/>");
		return b.toString();
	}
}
