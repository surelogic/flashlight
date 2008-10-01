package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.ICallLocation;

/**
 * Only intended to be used inside of TraceNode
 * 
 * @author Edwin.Chan
 */
public interface ITraceNode extends ICallLocation {
	ITraceNode getParent();
	TraceNode getNode();
	ITraceNode getCallee(ICallLocation key);
}
