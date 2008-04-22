package com.surelogic._flashlight;

/**
 * This event notes that all read and writes to a particular field occurred
 * within a single thread. The {@link Refinery} makes a best effort to remove
 * cached events about single-threaded fields, however, because it has limited
 * memory some events might have been output. Data prep can safely removed all
 * events about any single-threaded field from the data.
 */
abstract class SingleThreadedField extends ObservationalEvent {

	private final ObservedField f_field;

	ObservedField getField() {
		return f_field;
	}

	SingleThreadedField(final ObservedField field) {
		assert field != null;
		f_field = field;
	}

	protected final void addField(final StringBuilder b) {
		Entities.addAttribute("field", f_field.getId(), b);
	}
}
