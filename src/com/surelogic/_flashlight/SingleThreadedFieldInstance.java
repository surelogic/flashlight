package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

final class SingleThreadedFieldInstance extends SingleThreadedField {

	private final ObjectPhantomReference f_receiver;

	PhantomReference getReceiver() {
		return f_receiver;
	}

	SingleThreadedFieldInstance(final ObservedField field,
			final ObjectPhantomReference receiver) {
		super(field);
		f_receiver = receiver;
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
		Entities.addAttribute("receiver", f_receiver.getId(), b);
		b.append("/>");
		return b.toString();
	}
	
	@Override
	public int hashCode() {
		return getField().hashCode() + f_receiver.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FieldAccessInstance) {
			FieldAccessInstance s = (FieldAccessInstance) o;
			return this.getField() == s.getField() &&
			       this.f_receiver == s.getReceiver();
		}
		else if (o instanceof SingleThreadedFieldInstance) {
			SingleThreadedFieldInstance s = (SingleThreadedFieldInstance) o;
			return this.getField() == s.getField() &&
		           this.f_receiver == s.getReceiver();
		}
		return false;
	}
}
