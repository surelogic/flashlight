/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ThreadLockSet {
	private final HashSet<Long> locks;
	private final String thread;
	private final long threadId;
	private Map<Long, Set<Long>> staticLockSets;
	private Map<Long, Map<Long, Set<Long>>> lockSets;

	ThreadLockSet(final String threadName, final long threadId) {
		thread = threadName;
		this.threadId = threadId;
		locks = new HashSet<Long>();
		lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		staticLockSets = new HashMap<Long, Set<Long>>();
	}

	/**
	 * This method should be called whenever a field access is made.
	 * 
	 * @param fieldId
	 * @param receiverId
	 */
	synchronized void field(final long fieldId, final long receiverId) {
		Map<Long, Set<Long>> receiverSets = lockSets.get(receiverId);
		if (receiverSets == null) {
			receiverSets = new HashMap<Long, Set<Long>>();
			lockSets.put(fieldId, receiverSets);
		}
		final Set<Long> lockSet = receiverSets.get(fieldId);
		if (lockSet == null) {
			receiverSets.put(fieldId, new HashSet<Long>(locks));
		} else {
			lockSet.retainAll(locks);
		}
	}

	/**
	 * This method should be called whenever a static field access is made
	 * 
	 * @param fieldId
	 */
	synchronized void field(final long fieldId) {
		final Set<Long> lockSet = staticLockSets.get(fieldId);
		if (lockSet == null) {
			staticLockSets.put(fieldId, new HashSet<Long>(locks));
		} else {
			lockSet.retainAll(locks);
		}
	}

	/**
	 * This method should be called whenever a lock is acquired.
	 * 
	 * @param lock
	 */
	synchronized void enterLock(final long lock) {
		locks.add(lock);
	}

	/**
	 * This method should be called whenever a lock is released.
	 * 
	 * @param lock
	 */
	synchronized void leaveLock(final long lock) {
		locks.remove(lock);
	}

	String getThread() {
		return thread;
	}

	long getThreadId() {
		return threadId;
	}

	/**
	 * Clears and returns the currently accumulated lock set information
	 * 
	 * @return
	 */
	public Map<Long, Set<Long>> clearStaticLockSets() {
		try {
			return staticLockSets;
		} finally {
			staticLockSets = new HashMap<Long, Set<Long>>();
		}
	}

	/**
	 * Clears and returns the currently accumulated lock set information
	 * 
	 * @return
	 */
	public Map<Long, Map<Long, Set<Long>>> clearLockSets() {
		try {
			return lockSets;
		} finally {
			lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		}
	}

}