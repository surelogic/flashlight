package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.PostMortemStore;

/**
 * Only intended to be used inside of TraceNode
 * 
 * @author Edwin.Chan
 */
interface ITraceNode {
	ITraceNode pushCallee(long siteId);

	ITraceNode popParent();

	ITraceNode peekParent();

	TraceNode getNode(PostMortemStore.State state);

	ITraceNode getCallee(long key);
	// int getAndClearUnpropagated();
	// int addToUnpropagated(int count);
}
