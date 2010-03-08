package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
}
