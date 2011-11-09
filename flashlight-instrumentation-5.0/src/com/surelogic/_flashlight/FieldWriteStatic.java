package com.surelogic._flashlight;

final class FieldWriteStatic extends FieldAccessStatic {

	FieldWriteStatic(final long field, final long siteId,
			final PostMortemStore.State state, final boolean underConstruction) {
		super(field, siteId, state, underConstruction);
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
		addClassUnderConstruction(b);
		b.append("/>");
		return b.toString();
	}
}
