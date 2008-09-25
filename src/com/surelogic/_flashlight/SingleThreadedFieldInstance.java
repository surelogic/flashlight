package com.surelogic._flashlight;

final class SingleThreadedFieldInstance extends SingleThreadedField {

	private final ObjectPhantomReference f_receiver;

	IdPhantomReference getReceiver() {
		return f_receiver;
	}

	SingleThreadedFieldInstance(final long field,
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
		return (int) (getFieldId() + f_receiver.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof FieldAccessInstance) {
			FieldAccessInstance s = (FieldAccessInstance) o;
			return this.getFieldId() == s.getFieldId() &&
			       this.f_receiver == s.getReceiver();
		}
		else if (o instanceof SingleThreadedFieldInstance) {
			SingleThreadedFieldInstance s = (SingleThreadedFieldInstance) o;
			return this.getFieldId() == s.getFieldId() &&
		           this.f_receiver == s.getReceiver();
		}
		return false;
	}
}
