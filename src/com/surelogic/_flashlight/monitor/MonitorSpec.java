package com.surelogic._flashlight.monitor;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.surelogic._flashlight.FieldDef;
import com.surelogic._flashlight.FieldDefs;

/**
 * 
 * @author nathan
 * 
 */
class MonitorSpec {

	private final String fieldSpec;
	private final Set<Long> fieldIds;

	MonitorSpec(final String fieldSpec, final FieldDefs defs) {
		this.fieldSpec = fieldSpec;
		this.fieldIds = new HashSet<Long>();
		final Pattern c = Pattern.compile(fieldSpec);
		for (final Entry<Long, FieldDef> field : defs.entrySet()) {
			if (c.matcher(field.getValue().getQualifiedFieldName()).matches()) {
				fieldIds.add(field.getKey());
			}
		}
	}

	/**
	 * Whether or not the current field is being monitored.
	 * 
	 * @param fieldId
	 * @return
	 */
	boolean isMonitoring(final long fieldId) {
		return fieldIds.contains(fieldId);
	}

	public String getFieldSpec() {
		return fieldSpec;
	}

}
