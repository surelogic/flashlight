package com.surelogic._flashlight.trace;

public abstract class AbstractPlaceholder implements ITraceNode {	
	final ITraceNode f_caller;
	
	AbstractPlaceholder(ITraceNode caller) {
		f_caller = caller;
	}
	
	public final ITraceNode getCallee(long key) {
		return null;
	}
	
	public ITraceNode pushCallee(long siteId) {
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
	
	public final int addToUnpropagated(int count) {
		return 0;
	}
}
