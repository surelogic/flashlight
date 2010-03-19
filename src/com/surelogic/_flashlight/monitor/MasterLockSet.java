package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * The master lock set analysis.
 * 
 * @author nathan
 * 
 */
public class MasterLockSet {
	private final Map<Long, Set<Long>> staticLockSets;
	private final Map<Long, Map<Long, Set<Long>>> lockSets;
	private final SharedFields shared;

	MasterLockSet(final SharedFields sf) {
		lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		staticLockSets = new HashMap<Long, Set<Long>>();
		this.shared = sf;
	}

	/**
	 * drain grabs the lock set results from the given LockSets object and
	 * merges them with its results. The given LockSets object is reset to a
	 * pristine state, although the locks currently held is kept.
	 * 
	 * @param other
	 */
	synchronized void drain(final ThreadLockSet other) {
		Map<Long, Set<Long>> otherStaticLockSets;
		Map<Long, Map<Long, Set<Long>>> otherLockSets;
		long threadId;
		synchronized (other) {
			otherStaticLockSets = other.clearStaticLockSets();
			otherLockSets = other.clearLockSets();
			threadId = other.getThreadId();
		}
		for (final Entry<Long, Set<Long>> e : otherStaticLockSets.entrySet()) {
			final long fieldId = e.getKey();
			shared.sharedField(fieldId, threadId);
			final Set<Long> otherLocks = e.getValue();
			final Set<Long> locks = staticLockSets.get(fieldId);
			if (locks == null) {
				staticLockSets.put(fieldId, otherLocks);
			} else {
				locks.retainAll(otherLocks);
			}
		}
		for (final Entry<Long, Map<Long, Set<Long>>> e : otherLockSets
				.entrySet()) {
			final long receiverId = e.getKey();
			Map<Long, Set<Long>> receiverMap = lockSets.get(receiverId);
			if (receiverMap == null) {
				receiverMap = new HashMap<Long, Set<Long>>();
				lockSets.put(receiverId, receiverMap);
			}
			for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
				final long fieldId = e1.getKey();
				shared.sharedField(receiverId, fieldId, threadId);
				final Set<Long> otherLocks = e1.getValue();
				final Set<Long> locks = receiverMap.get(fieldId);
				if (locks == null) {
					receiverMap.put(fieldId, otherLocks);
				} else {
					locks.retainAll(otherLocks);
				}
			}
		}
	}

	/**
	 * Purge the given receiver from this lock set analysis and return the
	 * results. This method should be called when an object is garbage
	 * collected.
	 * 
	 * @param receiverId
	 * @return a map of lock sets keyed by field id
	 */
	synchronized Map<Long, Set<Long>> purge(final long receiverId) {
		final Map<Long, Set<Long>> map = lockSets.get(receiverId);
		lockSets.remove(receiverId);
		return map;
	}

	Map<Long, Set<Long>> getStaticLockSets() {
		return staticLockSets;
	}

	Map<Long, Map<Long, Set<Long>>> getLockSets() {
		return lockSets;
	}

}
