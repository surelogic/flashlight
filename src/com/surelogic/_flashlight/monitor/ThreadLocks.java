/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ThreadLocks {
	private final HashSet<Long> locks;
	private final String thread;
	private final long threadId;
	private Map<Long, Set<Long>> staticLockSets;
	private Map<Long, Map<Long, Set<Long>>> lockSets;
	private Set<LockStack> stacks;
	private LockStack stack;

	ThreadLocks(final String threadName, final long threadId) {
		thread = threadName;
		this.threadId = threadId;
		locks = new HashSet<Long>();
		lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		staticLockSets = new HashMap<Long, Set<Long>>();
		stack = new LockStack();
		stacks = new HashSet<LockStack>();
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
		if (locks.add(lock)) {
			stack = stack.acquire(lock);
			stacks.add(stack);
		}
	}

	/**
	 * This method should be called whenever a lock is released.
	 * 
	 * @param lock
	 */
	synchronized void leaveLock(final long lock) {
		locks.remove(lock);
		stack = stack.release(lock);
		stacks.add(stack);
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
	Map<Long, Set<Long>> clearStaticLockSets() {
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
	Map<Long, Map<Long, Set<Long>>> clearLockSets() {
		try {
			return lockSets;
		} finally {
			lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		}
	}

	public Set<LockStack> clearLockStacks() {
		try {
			return stacks;
		} finally {
			stacks = new HashSet<LockStack>();
		}
	}

	static class LockStack {
		static final int HEAD = -1;
		final LockStack parentLock;
		final long lockId;

		LockStack() {
			lockId = HEAD;
			parentLock = null;
			hash = _hashCode();
		}

		public LockStack getParentLock() {
			return parentLock;
		}

		public long getLockId() {
			return lockId;
		}

		LockStack release(final long lock) {
			if (lockId == lock) {
				return parentLock;
			} else if (lockId == HEAD) {
				return this;
			} else {
				return new LockStack(parentLock.release(lock), lockId);
			}
		}

		LockStack(final LockStack stack, final long id) {
			parentLock = stack;
			lockId = id;
			hash = _hashCode();
		}

		LockStack acquire(final long lockId) {
			if (contains(lockId)) {
				return this;
			} else {
				return new LockStack(this, lockId);
			}
		}

		boolean contains(final long testId) {
			if (lockId == testId) {
				return true;
			} else if (lockId == HEAD) {
				return false;
			} else {
				return parentLock.contains(testId);
			}
		}

		private final int hash;

		@Override
		public int hashCode() {
			return hash;
		}

		public int _hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (lockId ^ lockId >>> 32);
			result = prime * result
					+ (parentLock == null ? 0 : parentLock.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			final LockStack other = (LockStack) obj;
			if (lockId != other.lockId) {
				return false;
			}
			if (parentLock == null) {
				if (other.parentLock != null) {
					return false;
				}
			} else if (!parentLock.equals(other.parentLock)) {
				return false;
			}
			return true;
		}

	}

}