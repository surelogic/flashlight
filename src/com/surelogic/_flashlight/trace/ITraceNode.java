package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.ICallLocation;

/**
 * Only intended to be used inside of TraceNode
 * 
 * @author Edwin.Chan
 */
interface ITraceNode extends ICallLocation {
	ITraceNode getParent();
	TraceNode getNode(TraceNode.Header header);
	ITraceNode getCallee(long key);
	//int getAndClearUnpropagated();
	//int addToUnpropagated(int count);
}
