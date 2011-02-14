package com.surelogic._flashlight;

/**
 * These packages are selected to be passed through the filter
 */
public class SelectedPackage extends TimedEvent {
	final String name;

	SelectedPackage(final String name) {
		super(System.nanoTime());
		this.name = name;
	}

	@Override
	void accept(final EventVisitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("<selected-package");
		addNanoTime(b);
		Entities.addAttribute("package", name, b);
		b.append("/>");
		return b.toString();
	}
}
