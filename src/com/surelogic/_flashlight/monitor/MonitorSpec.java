package com.surelogic._flashlight.monitor;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * 
 * @author nathan
 * 
 */
class MonitorSpec {

	final String fieldSpec;
	final FieldDefs defs;
	final Set<Integer> fieldIds;

	MonitorSpec(final String fieldSpec, final FieldDefs defs) {
		this.fieldSpec = fieldSpec;
		this.defs = defs;
		this.fieldIds = new HashSet<Integer>();
		final Pattern c = Pattern.compile(fieldSpec);
		for (final Entry<Integer, String> field : defs.entrySet()) {
			if (c.matcher(field.getValue()).matches()) {
				fieldIds.add(field.getKey());
			}
		}
	}

	boolean isMonitoring(final long fieldId) {
		return fieldIds.contains((int) fieldId);
	}
}
