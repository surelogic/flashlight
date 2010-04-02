package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class SharedFields {

	final Map<Long, Set<Long>> sharedStatics;

	final Map<Long, Map<Long, Set<Long>>> sharedFields;

	SharedFields() {
		this.sharedStatics = new HashMap<Long, Set<Long>>();
		this.sharedFields = new HashMap<Long, Map<Long, Set<Long>>>();
	}

	/**
	 * Records the field as having been accessed by the given thread
	 * 
	 * @param fieldId
	 * @param threadId
	 */
	void sharedField(final long fieldId, final long threadId) {
		Set<Long> set = sharedStatics.get(fieldId);
		if (set == null) {
			set = new HashSet<Long>();
			sharedStatics.put(fieldId, set);
		}
		set.add(threadId);
	}

	/**
	 * Records the field as having been accessed by the given thread
	 * 
	 * @param fieldId
	 * @param threadId
	 */
	void sharedField(final long receiverId, final long fieldId,
			final long threadId) {
		Map<Long, Set<Long>> shared = sharedFields.get(receiverId);
		if (shared == null) {
			shared = new HashMap<Long, Set<Long>>();
		}
		Set<Long> set = shared.get(fieldId);
		if (set == null) {
			set = new HashSet<Long>();
			shared.put(fieldId, set);
		}
		set.add(threadId);
	}

	boolean isShared(final long fieldId) {
		final Set<Long> set = sharedStatics.get(fieldId);
		if (set == null || set.size() <= 1) {
			return false;
		}
		return true;
	}

	boolean isShared(final long receiverId, final long fieldId) {
		final Map<Long, Set<Long>> shared = sharedFields.get(receiverId);
		if (shared == null) {
			return false;
		}
		final Set<Long> set = shared.get(fieldId);
		if (set == null || set.size() <= 1) {
			return false;
		}
		return true;
	}

	/**
	 * Remove the object with the given id from consideration. This should be
	 * called when an object is garbage collected.
	 * 
	 * @param receiverId
	 */
	void remove(final long receiverId) {
		sharedFields.remove(receiverId);
	}

	/**
	 * Calculate the set of fields that are shared my multiple threads.
	 * 
	 * @return
	 */
	Set<Long> calculateSharedFields() {
		final Set<Long> set = new HashSet<Long>();
		for (final Entry<Long, Set<Long>> e : sharedStatics.entrySet()) {
			final long fieldId = e.getKey();
			if (e.getValue().size() > 1) {
				set.add(fieldId);
			}
		}
		for (final Entry<Long, Map<Long, Set<Long>>> e : sharedFields
				.entrySet()) {
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				final long fieldId = e1.getKey();
				if (e1.getValue().size() > 1) {
					set.add(fieldId);
				}
			}
		}
		return set;
	}

	/**
	 * Calculate the set of fields that are not shared my multiple threads.
	 * 
	 * @return
	 */
	Set<Long> calculateUnsharedFields() {
		final Set<Long> set = new HashSet<Long>();
		for (final Entry<Long, Set<Long>> e : sharedStatics.entrySet()) {
			final long fieldId = e.getKey();
			if (e.getValue().size() <= 1) {
				set.add(fieldId);
			}
		}
		for (final Entry<Long, Map<Long, Set<Long>>> e : sharedFields
				.entrySet()) {
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				final long fieldId = e1.getKey();
				if (e1.getValue().size() <= 1) {
					set.add(fieldId);
				}
			}
		}
		return set;
	}
}
