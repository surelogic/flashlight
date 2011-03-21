/**
 * 
 */
package com.surelogic._flashlight.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

final class ThreadLocks {

    private final ConcurrentMap<Long, ReadWriteLockIds> rwLocks;

    private final HashSet<Long> locks;
    private final String thread;
    private final long threadId;
    private final boolean isEDT;
    private Map<Long, Set<Long>> staticLockSets;
    private Map<Long, Map<Long, Set<Long>>> lockSets;
    private Set<LockStack> stacks;
    private Set<Long> sharedStatics;
    private Map<Long, Set<Long>> shared;
    private LockStack stack;

    ThreadLocks(final String threadName, final long threadId,
            final boolean isEDT,
            final ConcurrentMap<Long, ReadWriteLockIds> rwLocks) {
        thread = threadName;
        this.threadId = threadId;
        locks = new HashSet<Long>();
        lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
        staticLockSets = new HashMap<Long, Set<Long>>();
        stack = new LockStack();
        stacks = new HashSet<LockStack>();
        sharedStatics = new HashSet<Long>();
        shared = new HashMap<Long, Set<Long>>();
        this.rwLocks = rwLocks;
        this.isEDT = isEDT;
    }

    /**
     * This method should be called whenever a field access is made.
     * 
     * @param fieldId
     * @param receiverId
     */
    synchronized void field(final long fieldId, final long receiverId,
            final boolean underConstruction) {
        Set<Long> set = shared.get(receiverId);
        if (set == null) {
            set = new HashSet<Long>();
            shared.put(receiverId, set);
        }
        set.add(fieldId);
        if (!underConstruction) {
            Map<Long, Set<Long>> receiverMap = lockSets.get(receiverId);
            if (receiverMap == null) {
                receiverMap = new HashMap<Long, Set<Long>>();
                lockSets.put(receiverId, receiverMap);
            }
            final Set<Long> lockSet = receiverMap.get(fieldId);
            if (lockSet == null) {
                receiverMap.put(fieldId, new HashSet<Long>(locks));
            } else {
                lockSet.retainAll(locks);
            }
        }
    }

    /**
     * This method should be called whenever a static field access is made
     * 
     * @param fieldId
     */
    synchronized void field(final long fieldId, final boolean underConstruction) {
        sharedStatics.add(fieldId);
        if (!underConstruction) {
            final Set<Long> lockSet = staticLockSets.get(fieldId);
            if (lockSet == null) {
                staticLockSets.put(fieldId, new HashSet<Long>(locks));
            } else {
                lockSet.retainAll(locks);
            }
        }
    }

    /**
     * This method should be called whenever a lock is acquired.
     * 
     * @param lock
     */
    synchronized void enterLock(long lock) {
        final ReadWriteLockIds rw = rwLocks.get(lock);
        lock = rw == null ? lock : rw.getId();
        locks.add(lock);
        stack = stack.acquire(lock);
        stacks.add(stack);
    }

    /**
     * This method should be called whenever a lock is released.
     * 
     * @param lock
     */
    synchronized void leaveLock(long lock) {
        final ReadWriteLockIds rw = rwLocks.get(lock);
        lock = rw == null ? lock : rw.getId();
        stack = stack.release(lock);
        if (stack.count(lock) == 0) {
            locks.remove(lock);
        }
        stacks.add(stack);
    }

    String getThread() {
        return thread;
    }

    long getThreadId() {
        return threadId;
    }

    boolean isEDT() {
        return isEDT;
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

    Set<LockStack> clearLockStacks() {
        try {
            return stacks;
        } finally {
            stacks = new HashSet<LockStack>();
        }
    }

    Set<Long> clearSharedStatics() {
        try {
            return sharedStatics;
        } finally {
            sharedStatics = new HashSet<Long>();
        }
    }

    Map<Long, Set<Long>> clearShared() {
        try {
            return shared;
        } finally {
            shared = new HashMap<Long, Set<Long>>();
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
            return new LockStack(this, lockId);
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

        int count(final long testId) {
            if (lockId == testId) {
                return 1 + parentLock.count(testId);
            } else if (lockId == HEAD) {
                return 0;
            } else {
                return parentLock.count(testId);
            }
        }

        private final int hash;

        @Override
        public int hashCode() {
            return hash;
        }

        private int _hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + hash;
            result = prime * result + (int) (lockId ^ lockId >>> 32);
            result = prime * result
                    + (parentLock == null ? 0 : parentLock.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LockStack other = (LockStack) obj;
            if (hash != other.hash) {
                return false;
            }
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