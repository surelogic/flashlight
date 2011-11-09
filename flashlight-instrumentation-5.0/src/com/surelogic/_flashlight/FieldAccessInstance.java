package com.surelogic._flashlight;

abstract class FieldAccessInstance extends FieldAccess {

	private final ObjectPhantomReference f_receiver;

	IdPhantomReference getReceiver() {
		return f_receiver;
	}

	private final boolean f_receiverUnderConstruction;

	boolean receiverUnderConstruction() {
		return f_receiverUnderConstruction;
	}

	FieldAccessInstance(final Object receiver, final long field,
			final long siteId, final PostMortemStore.State state) {
		super(field, siteId, state);
		f_receiver = Phantom.ofObject(receiver);
		f_receiverUnderConstruction = f_receiver.isUnderConstruction();
	}

	protected final void addReceiver(final StringBuilder b) {
		Entities.addAttribute("receiver", f_receiver.getId(), b);
		if (f_receiverUnderConstruction) {
			Entities.addAttribute("under-construction", "yes", b);
		}
	}

	@Override
	IFieldInfo getFieldInfo() {
		return f_receiver.getFieldInfo();
	}

	@Override
	public int hashCode() {
		return (int) (getFieldId() + f_receiver.hashCode());
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof FieldAccessInstance) {
			FieldAccessInstance s = (FieldAccessInstance) o;
			return this.getFieldId() == s.getFieldId()
					&& this.f_receiver == s.getReceiver();
		} else if (o instanceof SingleThreadedFieldInstance) {
			SingleThreadedFieldInstance s = (SingleThreadedFieldInstance) o;
			return this.getFieldId() == s.getFieldId()
					&& this.f_receiver == s.getReceiver();
		}
		return false;
	}

	abstract boolean isWrite();
}
