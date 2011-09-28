package com.surelogic._flashlight.monitor;

import java.util.HashSet;
import java.util.Set;

import com.surelogic._flashlight.common.FieldDef;
import com.surelogic._flashlight.common.FieldDefs;

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

	/**
	 * Returns the alerts that are in this object, but not in the given
	 * AlertInfo object.
	 * 
	 * @param info
	 * @return
	 */
	public AlertInfo alertsSince(final AlertInfo info) {
		final HashSet<FieldDef> edts = new HashSet<FieldDef>(edtViolations);
		final HashSet<FieldDef> shared = new HashSet<FieldDef>(
				sharedFieldViolations);
		final HashSet<FieldDef> lockSet = new HashSet<FieldDef>(
				lockSetViolations);
		edts.removeAll(info.edtViolations);
		shared.removeAll(info.sharedFieldViolations);
		lockSet.removeAll(info.lockSetViolations);
		return new AlertInfo(edts, shared, lockSet);
	}

	public boolean isEmpty() {
		return edtViolations.isEmpty() && sharedFieldViolations.isEmpty()
				&& lockSetViolations.isEmpty();
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		if (edtViolations.size() > 0) {
			b.append("Swing Event Dispatch Thread violations:\n");
			FieldDefs.appendFieldDefs(b, edtViolations);
		}
		if (sharedFieldViolations.size() > 0) {
			b.append("Shared field alerts:\n");
			FieldDefs.appendFieldDefs(b, sharedFieldViolations);
		}
		if (lockSetViolations.size() > 0) {
			b.append("Empty lock set alerts:\n");
			FieldDefs.appendFieldDefs(b, lockSetViolations);
		}
		return b.toString();
	}
}
