package com.surelogic._flashlight.monitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.surelogic._flashlight.FieldDef;
import com.surelogic._flashlight.FieldDefs;

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
		b.append(raceInfo());
		b.append("\n");
		final Set<FieldDef> activeProtectedFields = activeProtectedFields();
		if (activeProtectedFields.isEmpty()) {
			b.append("No actively protected fields:\n");
		} else {
			b.append("Actively protected fields:\n");
			FieldDefs.appendFieldDefs(b, activeProtectedFields);
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
	 * The set of fields where potential race conditions have currently been
	 * observed.
	 * 
	 * @return
	 */
	Set<FieldDef> potentialRaceConditions() {
		final Set<FieldDef> raceFields = new HashSet<FieldDef>();
		for (final Entry<Long, Set<Long>> e : statics.entrySet()) {
			if (e.getValue().isEmpty()) {
				final FieldDef fieldDef = defs.get(e.getKey());
				if (!(fieldDef.isFinal() || fieldDef.isVolatile())) {
					raceFields.add(fieldDef);
				}
			}
		}
		for (final Entry<Long, Map<Long, Set<Long>>> e : instances.entrySet()) {
			final FieldDef f = defs.get(e.getKey());
			if (!(f.isFinal() || f.isVolatile())) {
				for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
					if (e1.getValue().isEmpty()) {
						raceFields.add(f);
						break;
					}
				}
			}
		}
		return raceFields;
	}

	/**
	 * The set of fields that are protected by one or more locks at all times.
	 * This may include non-static fields that are sometimes observed to be
	 * protected, and sometimes not.
	 * 
	 * @return
	 */
	Set<FieldDef> activeProtectedFields() {
		final Set<FieldDef> set = new HashSet<FieldDef>();
		for (final Entry<Long, Set<Long>> e : statics.entrySet()) {
			if (!e.getValue().isEmpty()) {
				set.add(defs.get(e.getKey()));
			}
		}
		for (final Entry<Long, Map<Long, Set<Long>>> e : instances.entrySet()) {
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				if (!e1.getValue().isEmpty()) {
					set.add(defs.get(e.getKey()));
					break;
				}
			}
		}
		return set;
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

	public String raceInfo() {
		final StringBuilder b = new StringBuilder();
		final Set<FieldDef> raceFields = potentialRaceConditions();
		if (!raceFields.isEmpty()) {
			b.append("Potential Race Conditions\n");
			hline(b);
			FieldDefs.appendFieldDefs(b, raceFields);
		} else {
			b.append("No potential race conditions found.\n");
		}
		return b.toString();
	}

	void hline(final StringBuilder b) {
		for (int i = 0; i < 80; i++) {
			b.append('-');
		}
		b.append('\n');
	}

}
