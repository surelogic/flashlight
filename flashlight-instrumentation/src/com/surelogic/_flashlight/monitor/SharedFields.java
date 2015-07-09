package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.surelogic._flashlight.common.FieldDef;

/**
 * Keeps track of what fields are currently shared by what threads.
 * 
 * @author nathan
 * 
 */
public class SharedFields {

	final Map<Long, Set<Long>> sharedStatics;

	final Map<Long, Map<Long, Set<Long>>> sharedFieldsByReceiver;

	final Map<Long, Map<Long, Set<Long>>> sharedFieldsByField;

	SharedFields() {
		this.sharedStatics = new HashMap<Long, Set<Long>>();
		this.sharedFieldsByReceiver = new HashMap<Long, Map<Long, Set<Long>>>();
		this.sharedFieldsByField = new HashMap<Long, Map<Long, Set<Long>>>();
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
		Map<Long, Set<Long>> shared = sharedFieldsByReceiver.get(receiverId);
		if (shared == null) {
			shared = new HashMap<Long, Set<Long>>();
			sharedFieldsByReceiver.put(receiverId, shared);
		}
		Set<Long> set = shared.get(fieldId);
		if (set == null) {
			set = new HashSet<Long>();
			shared.put(fieldId, set);
			Map<Long, Set<Long>> receiverMap = sharedFieldsByField.get(fieldId);
			if (receiverMap == null) {
				receiverMap = new HashMap<Long, Set<Long>>();
				sharedFieldsByField.put(fieldId, receiverMap);
			}
			receiverMap.put(receiverId, set);
		}
		set.add(threadId);
	}

	/**
	 * Whether or not the field matching this definition is ever shared.
	 * 
	 * @param field
	 * @return
	 */
	public boolean isShared(final FieldDef field) {
		if (field.isStatic()) {
			return isShared(field.getId());
		} else {
			final Map<Long, Set<Long>> map = sharedFieldsByField.get(field
					.getId());
			if (map != null) {
				for (final Set<Long> set : map.values()) {
					if (set.size() > 1) {
						return true;
					}
				}
			}
			return false;
		}
	}

	boolean isShared(final long fieldId) {
		final Set<Long> set = sharedStatics.get(fieldId);
		if (set == null || set.size() <= 1) {
			return false;
		}
		return true;
	}

	boolean isShared(final long receiverId, final long fieldId) {
		final Map<Long, Set<Long>> shared = sharedFieldsByReceiver
				.get(receiverId);
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
	 * Returns whether or not the given static field is confined to the given
	 * set of threads.
	 * 
	 * @param fieldId
	 * @param allowedThreads
	 * @return
	 */
	boolean isConfinedTo(final FieldDef field, final Set<Long> allowedThreads) {
		if (field.isStatic()) {
			final Set<Long> set = sharedStatics.get(field.getId());
			if (set != null) {
				return allowedThreads.containsAll(set);
			}
			return true;
		} else {
			final Map<Long, Set<Long>> map = sharedFieldsByField.get(field
					.getId());
			if (map != null) {
				for (final Set<Long> set : map.values()) {
					if (!allowedThreads.containsAll(set)) {
						return false;
					}
				}
			}
			return true;
		}
	}

	/**
	 * Remove the object with the given id from consideration. This should be
	 * called when an object is garbage collected.
	 * 
	 * @param receiverId
	 */
	void remove(final long receiverId) {
		final Map<Long, Set<Long>> remove = sharedFieldsByReceiver
				.remove(receiverId);
		if (remove != null) {
			for (final long fieldId : remove.keySet()) {
				sharedFieldsByField.get(fieldId).remove(receiverId);
			}
		}
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
		for (final Entry<Long, Map<Long, Set<Long>>> e : sharedFieldsByReceiver
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
		for (final Entry<Long, Map<Long, Set<Long>>> e : sharedFieldsByReceiver
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
