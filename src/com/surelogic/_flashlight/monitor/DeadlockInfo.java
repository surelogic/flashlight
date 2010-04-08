package com.surelogic._flashlight.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic._flashlight.monitor.ThreadLocks.LockStack;

public class DeadlockInfo {

	private final Set<LockStack> lockOrders;
	private final Set<Long> deadlocks;

	public DeadlockInfo(final Set<LockStack> lockOrders,
			final Set<Long> deadlocks) {
		this.lockOrders = lockOrders;
		this.deadlocks = deadlocks;
	}

	public Set<LockStack> getLockOrders() {
		return lockOrders;
	}

	public Set<Long> getDeadlocks() {
		return deadlocks;
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("Lock Orderings:\n");
		appendLockOrders(b, lockOrders);
		b.append("Potential Deadlocks:\n");
		final Set<String> deadlockNames = new HashSet<String>(deadlocks.size());
		for (final long l : deadlocks) {
			deadlockNames.add(objId(l));
		}
		b.append(deadlocks);
		b.append("\n");
		return b.toString();
	}

	private void appendLockOrders(final StringBuilder b,
			final Set<LockStack> lockOrders) {
		final List<String> strs = new ArrayList<String>();
		for (LockStack stack : lockOrders) {
			String s = "";
			for (; stack.getLockId() != LockStack.HEAD; stack = stack
					.getParentLock()) {
				s = "-> " + objId(stack.getLockId()) + s;
			}
			strs.add(s);
		}
		Collections.sort(strs);
		for (final String s : strs) {
			b.append(s);
			b.append('\n');
		}
	}

	String objId(final long obj) {
		return Long.toString(obj);
	}

}
