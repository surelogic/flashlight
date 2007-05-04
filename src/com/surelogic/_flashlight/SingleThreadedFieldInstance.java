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
}
