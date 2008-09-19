package com.surelogic._flashlight;

final class SingleThreadedFieldStatic extends SingleThreadedField {

	SingleThreadedFieldStatic(final long field) {
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
		return (int) getFieldId();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FieldAccessStatic) {
			FieldAccessStatic s = (FieldAccessStatic) o;
			return this.getFieldId() == s.getFieldId();
		}
		else if (o instanceof SingleThreadedFieldStatic) {
			SingleThreadedFieldStatic s = (SingleThreadedFieldStatic) o;
			return this.getFieldId() == s.getFieldId();
		}
		return false;
	}
}
