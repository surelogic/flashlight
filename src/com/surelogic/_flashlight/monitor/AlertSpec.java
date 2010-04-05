package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class AlertSpec {

	private final String edtFields;
	private final String sharedFields;
	private final String lockSetFields;
	private final List<FieldDef> edtFieldDefs;
	private final List<FieldDef> sharedFieldDefs;
	private final List<FieldDef> lockSetFieldDefs;

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
		sharedFieldDefs = new ArrayList<FieldDef>();
		lockSetFieldDefs = new ArrayList<FieldDef>();
		final Pattern edtPattern = Pattern.compile(edtFields);
		final Pattern sharedFieldPattern = Pattern.compile(sharedFields);
		final Pattern lockSetPattern = Pattern.compile(lockSetFields);
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

	List<FieldDef> getEDTFields() {
		return Collections.unmodifiableList(edtFieldDefs);
	}

	String getEDTSpec() {
		return edtFields;
	}
}
