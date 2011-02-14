package com.surelogic._flashlight;

/**
 * Used to signal to the {@link Refinery} and the {@link Depository} that
 * collection has been completed and they should terminate.
 */
final class FinalEvent extends TimedEvent {

	/**
	 * The singleton instance.
	 */
	static final FinalEvent FINAL_EVENT = new FinalEvent();

	private FinalEvent() {
		super(System.nanoTime());
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<final");
		addNanoTime(b);
		b.append("/>");
		return b.toString();
	}
}
