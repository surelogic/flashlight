package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import com.surelogic._flashlight.monitor.ThreadLocks.LockStack;

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
	private final Graph graph;
	private final ConcurrentMap<Long, ReadWriteLockIds> rwLocks;

	MasterLockSet(final SharedFields sf,
			final ConcurrentMap<Long, ReadWriteLockIds> rwLocks) {
		lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		staticLockSets = new HashMap<Long, Set<Long>>();
		this.shared = sf;
		this.rwLocks = rwLocks;
		graph = new Graph();
	}

	/**
	 * drain grabs the lock set results from the given LockSets object and
	 * merges them with its results. The given LockSets object is reset to a
	 * pristine state, although the locks currently held is kept.
	 * 
	 * @param other
	 */
	synchronized void drain(final ThreadLocks other) {
		Map<Long, Set<Long>> otherStaticLockSets;
		Map<Long, Map<Long, Set<Long>>> otherLockSets;
		Set<LockStack> otherStacks;
		Set<Long> otherSharedStatics;
		Map<Long, Set<Long>> otherShared;
		long threadId;
		synchronized (other) {
			otherStaticLockSets = other.clearStaticLockSets();
			otherLockSets = other.clearLockSets();
			otherStacks = other.clearLockStacks();
			otherSharedStatics = other.clearSharedStatics();
			otherShared = other.clearShared();
			threadId = other.getThreadId();
		}
		for (final long fieldId : otherSharedStatics) {
			shared.sharedField(fieldId, threadId);
		}
		for (final Entry<Long, Set<Long>> e : otherShared.entrySet()) {
			final long receiverId = e.getKey();
			for (final long fieldId : e.getValue()) {
				shared.sharedField(receiverId, fieldId, threadId);
			}
		}
		for (final Entry<Long, Set<Long>> e : otherStaticLockSets.entrySet()) {
			final long fieldId = e.getKey();
			final Set<Long> otherLocks = e.getValue();
			final Set<Long> toCheck = new HashSet<Long>(otherLocks.size());
			for (final long l : otherLocks) {
				final ReadWriteLockIds ids = rwLocks.get(l);
				toCheck.add(ids == null ? l : ids.getId());
			}
			final Set<Long> locks = staticLockSets.get(fieldId);
			if (locks == null) {
				staticLockSets.put(fieldId, toCheck);
			} else {
				locks.retainAll(toCheck);
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
				final Set<Long> otherLocks = e1.getValue();
				final Set<Long> toCheck = new HashSet<Long>(otherLocks.size());
				for (final long l : otherLocks) {
					final ReadWriteLockIds ids = rwLocks.get(l);
					toCheck.add(ids == null ? l : ids.getId());
				}
				final Set<Long> locks = receiverMap.get(fieldId);
				if (locks == null) {
					receiverMap.put(fieldId, toCheck);
				} else {
					locks.retainAll(toCheck);
				}
			}
		}
		for (final LockStack stack : otherStacks) {
			graph.add(stack);
		}
	}

	static class Graph {

		final Map<Long, Set<Long>> reachable;
		final Set<LockStack> stacks;

		Graph() {
			reachable = new HashMap<Long, Set<Long>>();
			stacks = new HashSet<LockStack>();
		}

		void add(LockStack stack) {
			stacks.add(stack);
			final List<Long> ids = new ArrayList<Long>();
			for (; stack.lockId != LockStack.HEAD; stack = stack.parentLock) {
				final long id = stack.lockId;
				Set<Long> set = reachable.get(id);
				if (set == null) {
					set = new HashSet<Long>();
					reachable.put(id, set);
				}
				set.addAll(ids);
				ids.add(id);
			}
		}

		Set<Long> cycles() {
			final Set<Long> cycleLocks = new HashSet<Long>();
			for (final Entry<Long, Set<Long>> vertex : reachable.entrySet()) {
				for (final long lock : vertex.getValue()) {
					if (reachable.get(lock).contains(vertex.getKey())) {
						cycleLocks.add(lock);
						cycleLocks.add(vertex.getKey());
					}
				}
			}
			return cycleLocks;
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

	public Set<LockStack> getLockOrders() {
		return Collections.unmodifiableSet(graph.stacks);
	}

	public Set<Long> getDeadlocks() {
		return graph.cycles();
	}

}
