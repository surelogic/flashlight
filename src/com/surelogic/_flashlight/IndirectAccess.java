package com.surelogic._flashlight;

public class IndirectAccess extends TracedEvent {
	private final ObjectPhantomReference f_receiver;

	IdPhantomReference getReceiver() {
		return f_receiver;
	}
	
	IndirectAccess(final Object receiver, final long siteId, Store.State state) {
		super(siteId, state);
		f_receiver = Phantom.ofObject(receiver);
	}

	@Override
	void accept(EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder(128);
		b.append("<indirect-access");
		addNanoTime(b);
		addThread(b);
		addReceiver(b);
		b.append("/>");
		return b.toString();
	}
	
	protected final void addReceiver(final StringBuilder b) {
		Entities.addAttribute("receiver", f_receiver.getId(), b);
	}
}
