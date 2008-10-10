package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.*;

public class Placeholder implements ITraceNode {
	final long f_siteId;
	final ITraceNode f_caller;
	
	Placeholder(long siteId, ITraceNode caller) {
		f_siteId = siteId;
		f_caller = caller;
	}
	
	public TraceNode getNode(Store.State state) {
		// First, try to see if I've cached a matching TraceNode
		TraceNode caller;
		ITraceNode callee;
		if (f_caller != null) {			
			// There's already a caller
			caller = f_caller.getNode(state);			
			callee = caller.getCallee(this.f_siteId);
		} else {
			// No caller yet
			caller = null;
			synchronized (TraceNode.roots) {	
				callee = TraceNode.roots.get(this.f_siteId);	
			}
		}
		if (callee != null) {
			return callee.getNode(state);
		}		
		return TraceNode.newTraceNode(caller, f_siteId, state);
	}
	
	public ITraceNode getCallee(long key) {
		return null;
	}
	
	public ITraceNode getParent() {
		return f_caller;
	}

	public final long getSiteId() {
		return f_siteId;
	}
	
	@Override
	public int hashCode() {
		return (int) f_siteId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ICallLocation) {
			ICallLocation bt = (ICallLocation) o;
            return bt.getSiteId() == getSiteId();
 		}
		return false;
	}
	
	public int getAndClearUnpropagated() {
		return 0;
	}
	
	public int addToUnpropagated(int count) {
		return 0;
	}
}
