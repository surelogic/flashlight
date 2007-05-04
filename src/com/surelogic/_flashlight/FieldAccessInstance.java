package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;

abstract class FieldAccessInstance extends FieldAccess {

	private final ObjectPhantomReference f_receiver;

	PhantomReference getReceiver() {
		return f_receiver;
	}

	FieldAccessInstance(final Object receiver, final ObservedField field,
			final SrcLoc location) {
		super(field, location);
		f_receiver = Phantom.ofObject(receiver);
	}

	@Override
	KeyField getKey() {
		return new KeyFieldInstance(getField(), f_receiver);
	}

	protected final void addReceiver(final StringBuilder b) {
		Entities.addAttribute("receiver", f_receiver.getId(), b);
	}
}
