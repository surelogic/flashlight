package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Represents the current state of all lock sets observed by the flashlight
 * monitor.
 * 
 * @author nathan
 * 
 */
public class LockSetInfo {

	private final Map<Long, Set<Long>> statics;
	private final Map<Long, Map<Long, Set<Long>>> instances;

	private final Set<Long> lockSetFields;
	private final Set<Long> noLockSetFields;
	private final FieldDefs defs;

	/**
	 * Constructs an informational object.
	 * 
	 * @param defs
	 * @param statics
	 * @param lockSetFields
	 *            a map of all lock sets for fields currently being observed in
	 *            the program, keyed by field first and then receiver
	 * @param noLockSetFields
	 * @param instances
	 */
	LockSetInfo(final FieldDefs defs, final Map<Long, Set<Long>> statics,
			final Set<Long> lockSetFields, final Set<Long> noLockSetFields,
			final Map<Long, Map<Long, Set<Long>>> instances) {
		this.lockSetFields = lockSetFields;
		this.noLockSetFields = noLockSetFields;
		this.defs = defs;
		this.statics = statics;
		this.instances = instances;
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("Static Fields:\n");
		final List<String> list = new ArrayList<String>();
		for (final Entry<Long, Set<Long>> e1 : statics.entrySet()) {
			final FieldDef fieldDef = defs.get(e1.getKey());
			list.add(fieldInfo(fieldDef));
		}
		Collections.sort(list);
		for (final String s : list) {
			b.append(s);
		}
		b.append("Instances:\n");
		b.append("Active Fields that have a lock set:\n");
		for (final Entry<Long, Map<Long, Set<Long>>> e : instances.entrySet()) {
			final FieldDef f = defs.get(e.getKey());
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				if (!e1.getValue().isEmpty()) {
					b.append(fieldInfo(f));
					break;
				}
			}
		}
		b.append("Active Fields that have no lock sets:\n");
		for (final Entry<Long, Map<Long, Set<Long>>> e : instances.entrySet()) {
			final FieldDef f = defs.get(e.getKey());
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				if (e1.getValue().isEmpty()) {
					b.append(fieldInfo(f));
					break;
				}
			}
		}
		b.append("Garbage Collected Fields:\n");
		b.append("Fields With No Lock Set:\n");
		defs.appendFields(b, noLockSetFields);
		b.append("Fields With Lock Sets:\n");
		defs.appendFields(b, lockSetFields);
		b.append("Fields that ALWAYS have a  Lock Set:\n");
		final HashSet<Long> instanceSet = new HashSet<Long>(lockSetFields);
		instanceSet.removeAll(noLockSetFields);
		defs.appendFields(b, instanceSet);
		return b.toString();
	}

	/**
	 * Whether or not the given field has always been observed to have a lock
	 * set.
	 * 
	 * @param field
	 * @return whether the field always has a lock set, or <code>true</code> if
	 *         it has never been observed.
	 */
	public boolean hasLockSet(final FieldDef field) {
		if (field.isStatic()) {
			final Set<Long> set = statics.get(field.getId());
			return set == null ? true : !set.isEmpty();
		}
		final Map<Long, Set<Long>> map = instances.get(field.getId());
		boolean hasLockSet = true;
		// Check all of the active receivers
		if (map != null) {
			for (final Set<Long> v : map.values()) {
				hasLockSet &= !v.isEmpty();
			}
		} else {
			// We return false if we have not found any lock sets
			hasLockSet &= lockSetFields.contains(field.getId());
		}
		hasLockSet &= !noLockSetFields.contains(field.getId());
		return hasLockSet;
	}

	/**
	 * Return a string displaying lock set information about the given field.
	 * 
	 * @param field
	 * @return
	 */
	public String lockSetInfo(final FieldDef field) {
		final StringBuilder b = new StringBuilder();
		if (field.isStatic()) {
			Set<Long> set = statics.get(field.getId());
			if (set == null || set.isEmpty()) {
				set = Collections.emptySet();
			}
			b.append(String.format("%s - %s\n", fieldInfo(field), set));
		} else {
			b.append(fieldInfo(field));
			final Map<Long, Set<Long>> map = instances.get(field.getId());
			if (map != null) {
				for (final Entry<Long, Set<Long>> receivers : map.entrySet()) {
					b.append(String.format("\t%s - %s\n", receivers.getKey(),
							receivers.getValue()));
				}
			}
		}
		return b.toString();
	}

	String fieldInfo(final FieldDef field) {
		return String.format("%s - %s\n", field.getQualifiedFieldName(), field
				.isFinal() ? "final" : "");
	}
}
