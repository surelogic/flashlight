package com.surelogic._flashlight.monitor;

import java.util.Set;

/**
 * Value object representing the current alerts in this program.
 * 
 * @author nathan
 * 
 */
public final class AlertInfo {

	private final Set<FieldDef> lockSetViolations;
	private final Set<FieldDef> sharedFieldViolations;
	private final Set<FieldDef> edtViolations;

	AlertInfo(final Set<FieldDef> edts, final Set<FieldDef> shared,
			final Set<FieldDef> lockSets) {
		this.edtViolations = edts;
		this.sharedFieldViolations = shared;
		this.lockSetViolations = lockSets;
	}

	public Set<FieldDef> getLockSetViolations() {
		return lockSetViolations;
	}

	public Set<FieldDef> getSharedFieldViolations() {
		return sharedFieldViolations;
	}

	public Set<FieldDef> getEdtViolations() {
		return edtViolations;
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("EDT thread alerts:\n");
		FieldDefs.appendFieldDefs(b, edtViolations);
		b.append("Shared field alerts:\n");
		FieldDefs.appendFieldDefs(b, sharedFieldViolations);
		b.append("Empty lock set alerts:\n");
		FieldDefs.appendFieldDefs(b, lockSetViolations);
		return b.toString();
	}
}
