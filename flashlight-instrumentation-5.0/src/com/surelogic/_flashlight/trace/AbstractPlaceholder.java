package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.PostMortemStore;

public abstract class AbstractPlaceholder implements ITraceNode {
	final ITraceNode f_caller;

	AbstractPlaceholder(final ITraceNode caller) {
		f_caller = caller;
	}

	public final ITraceNode getCallee(final long key) {
		return null;
	}

	static TraceNode getNode(final PostMortemStore.State state,
			final TraceNode caller, final long siteId) {
		// First, try to see if I've cached a matching TraceNode
		ITraceNode callee;
		if (caller != null) {
			// There's already a caller
			callee = caller.getCallee(siteId);
		} else {
			// No caller yet
			synchronized (TraceNode.roots) {
				callee = TraceNode.roots.get(siteId);
			}
		}
		if (callee != null) {
			return callee.getNode(state);
		}
		return TraceNode.newTraceNode(caller, siteId, state);
	}

	public ITraceNode pushCallee(final long siteId) {
		return new Placeholder(this, siteId);
	}

	public ITraceNode popParent() {
		return f_caller;
	}

	public final ITraceNode peekParent() {
		throw new UnsupportedOperationException();
	}

	public final int getAndClearUnpropagated() {
		return 0;
	}

	public final int addToUnpropagated(final int count) {
		return 0;
	}
}
