package com.surelogic._flashlight;

/**
 * This event notes that all read and writes to a particular field occurred
 * within a single thread. The {@link Refinery} makes a best effort to remove
 * cached events about single-threaded fields, however, because it has limited
 * memory some events might have been output. Data prep can safely removed all
 * events about any single-threaded field from the data.
 */
abstract class SingleThreadedField extends ObservationalEvent {

	private final long f_fieldId;

	long getFieldId() {
		return f_fieldId;
	}

	SingleThreadedField(final long fieldId) {
		f_fieldId = fieldId;
	}

	protected final void addField(final StringBuilder b) {
		Entities.addAttribute("field", f_fieldId, b);
	}
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object o);
}
