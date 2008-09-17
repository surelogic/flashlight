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
	
	@Override
	public int hashCode() {
		return getField().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FieldAccessStatic) {
			FieldAccessStatic s = (FieldAccessStatic) o;
			return this.getField() == s.getField();
		}
		else if (o instanceof SingleThreadedFieldStatic) {
			SingleThreadedFieldStatic s = (SingleThreadedFieldStatic) o;
			return this.getField() == s.getField();
		}
		return false;
	}
}
