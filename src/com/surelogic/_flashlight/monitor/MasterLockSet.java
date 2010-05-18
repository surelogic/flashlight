package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
	private final FieldDefs defs;
	private final SharedFields shared;
	private final Graph graph;

	/*
	 * Keeps track of the results from objects that have been garbage collected.
	 */
	private final Set<Long> lockSetFields;
	private final Set<Long> noLockSetFields;

	MasterLockSet(final FieldDefs defs, final SharedFields sf) {
		lockSets = new HashMap<Long, Map<Long, Set<Long>>>();
		staticLockSets = new HashMap<Long, Set<Long>>();
		this.shared = sf;
		this.defs = defs;
		graph = new Graph();
		lockSetFields = new HashSet<Long>();
		noLockSetFields = new HashSet<Long>();
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
				final Set<Long> otherLocks = e1.getValue();
				final Set<Long> locks = receiverMap.get(fieldId);
				if (locks == null) {
					receiverMap.put(fieldId, otherLocks);
				} else {
					locks.retainAll(otherLocks);
				}
			}
		}
		for (final LockStack stack : otherStacks) {
			graph.add(stack);
		}
	}

	static class Graph {

		final Map<Long, Set<Long>> reachable;
		final Map<Long, Set<Long>> reachedBy;
		final Set<LockStack> stacks;

		Graph() {
			reachable = new HashMap<Long, Set<Long>>();
			reachedBy = new HashMap<Long, Set<Long>>();
			stacks = new HashSet<LockStack>();
		}

		void add(LockStack stack) {
			stacks.add(stack);
			final List<Long> ids = new ArrayList<Long>();
			for (; stack.lockId != LockStack.HEAD; stack = stack.parentLock) {
				ids.add(stack.lockId);
			}
			final int len = ids.size();
			for (int i = 0; i < len; i++) {
				final long id = ids.get(i);
				Set<Long> set = reachable.get(id);
				if (set == null) {
					set = new HashSet<Long>();
					reachable.put(id, set);
				}
				set.addAll(ids.subList(0, i));
				set = reachedBy.get(id);
				if (set == null) {
					set = new HashSet<Long>();
					reachedBy.put(id, set);
				}
				set.addAll(ids.subList(i + 1, len));
			}
		}

		/**
		 * Compute the set of locks involved in some sort of deadlock cycle
		 * 
		 * @return
		 */
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

		/**
		 * Purges the given lock id from the graph. This will change the
		 * representation of the existing lock stacks.
		 * 
		 * @param lockId
		 */
		void purge(final long lockId) {
			final Set<Long> reached = reachedBy.remove(lockId);
			if (reached != null) {
				for (final long id : reached) {
					reachable.get(id).remove(lockId);
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
		final Map<Long, Set<Long>> fields = lockSets.get(receiverId);
		lockSets.remove(receiverId);
		if (fields != null) {
			for (final Entry<Long, Set<Long>> e : fields.entrySet()) {
				final long fieldId = e.getKey();
				if (shared.isShared(receiverId, fieldId)) {
					if (e.getValue().isEmpty()) {
						noLockSetFields.add(fieldId);
					} else {
						lockSetFields.add(fieldId);
					}
				}
			}
		}
		graph.purge(receiverId);
		return fields;
	}

	Set<LockStack> getLockOrders() {
		return Collections.unmodifiableSet(graph.stacks);
	}

	Set<Long> getDeadlocks() {
		return graph.cycles();
	}

	/**
	 * Return a {@link LockSetInfo} showing the lock sets for all fields that
	 * are actively shared in the program.
	 * 
	 * @return
	 */
	LockSetInfo getLockSetInfo() {
		final Map<Long, Set<Long>> statics = new HashMap<Long, Set<Long>>(
				staticLockSets.size());
		for (final Entry<Long, Set<Long>> e : staticLockSets.entrySet()) {
			if (shared.isShared(e.getKey())) {
				statics.put(e.getKey(), new HashSet<Long>(e.getValue()));
			}
		}
		// We reverse the lock set map here, as querying by field is generally
		// what we want
		final Map<Long, Map<Long, Set<Long>>> instances = new HashMap<Long, Map<Long, Set<Long>>>(
				lockSets.size());
		for (final Entry<Long, Map<Long, Set<Long>>> e : lockSets.entrySet()) {
			final long receiver = e.getKey();
			final Map<Long, Set<Long>> recMap = e.getValue();
			for (final Entry<Long, Set<Long>> e1 : recMap.entrySet()) {
				final long field = e1.getKey();
				if (shared.isShared(receiver, field)) {
					Map<Long, Set<Long>> fieldMap = instances.get(field);
					if (fieldMap == null) {
						fieldMap = new HashMap<Long, Set<Long>>();
						instances.put(field, fieldMap);
					}
					fieldMap.put(receiver, new HashSet<Long>(e1.getValue()));
				}
			}
		}
		return new LockSetInfo(defs, statics, new HashSet<Long>(lockSetFields),
				new HashSet<Long>(noLockSetFields), instances);
	}

}
