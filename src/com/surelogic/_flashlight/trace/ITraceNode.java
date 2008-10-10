package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.ICallLocation;
import com.surelogic._flashlight.Store;

/**
 * Only intended to be used inside of TraceNode
 * 
 * @author Edwin.Chan
 */
interface ITraceNode extends ICallLocation {
	ITraceNode getParent();
	TraceNode getNode(Store.State state);
	ITraceNode getCallee(long key);
	//int getAndClearUnpropagated();
	//int addToUnpropagated(int count);
}
