package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The conditions in which the user should be notified by the monitor.
 * 
 * @author nathan
 * 
 */
public class AlertSpec {

	private final String edtFields;
	private final String sharedFields;
	private final String lockSetFields;
	private final List<FieldDef> edtFieldDefs;
	private final List<FieldDef> sharedFieldDefs;
	private final List<FieldDef> lockSetFieldDefs;
	private final FieldDefs defs;

	AlertSpec(final FieldDefs defs) {
		this(System.getProperty("com.surelogic.swingFieldAlerts", ""), System
				.getProperty("com.surelogic.sharedFieldAlerts", ""), System
				.getProperty("com.surelogic.lockSetAlerts", ""), defs);
	}

	AlertSpec(final String edtFields, final String sharedFields,
			final String lockSetFields, final FieldDefs defs) {
		this.edtFields = edtFields;
		this.sharedFields = sharedFields;
		this.lockSetFields = lockSetFields;
		this.edtFieldDefs = new ArrayList<FieldDef>();
		this.defs = defs;
		sharedFieldDefs = new ArrayList<FieldDef>();
		lockSetFieldDefs = new ArrayList<FieldDef>();
		final Pattern edtPattern = Pattern.compile(edtFields == null ? ""
				: edtFields);
		final Pattern sharedFieldPattern = Pattern
				.compile(sharedFields == null ? "" : sharedFields);
		final Pattern lockSetPattern = Pattern
				.compile(lockSetFields == null ? "" : lockSetFields);
		for (final FieldDef field : defs.values()) {
			final String name = field.getQualifiedFieldName();
			if (edtPattern.matcher(name).matches()) {
				edtFieldDefs.add(field);
			}
			if (sharedFieldPattern.matcher(name).matches()) {
				sharedFieldDefs.add(field);
			}
			if (lockSetPattern.matcher(name).matches()) {
				lockSetFieldDefs.add(field);
			}
		}
	}

	public List<FieldDef> getEDTFields() {
		return Collections.unmodifiableList(edtFieldDefs);
	}

	public List<FieldDef> getSharedFields() {
		return Collections.unmodifiableList(sharedFieldDefs);
	}

	public List<FieldDef> getLockSetFields() {
		return Collections.unmodifiableList(lockSetFieldDefs);
	}

	public String getEDTSpec() {
		return edtFields;
	}

	public String getSharedSpec() {
		return sharedFields;
	}

	public String getLockSetSpec() {
		return lockSetFields;
	}

	/**
	 * Merge a new spec with the existing one. Old spec entries are kept if the
	 * new entry is null.
	 * 
	 * @param spec
	 * @return
	 */
	public AlertSpec merge(final AlertSpec spec) {
		final String newEdt = spec.edtFields == null ? edtFields
				: spec.edtFields;
		final String newShared = spec.sharedFields == null ? sharedFields
				: spec.sharedFields;
		final String newLockSet = spec.lockSetFields == null ? lockSetFields
				: spec.lockSetFields;
		return new AlertSpec(newEdt, newShared, newLockSet, spec.defs);
	}

}
