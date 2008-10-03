package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.*;

public class Placeholder implements ITraceNode {
	final ClassPhantomReference f_class;
	final int f_line;
	final ITraceNode f_caller;
	
	Placeholder(ClassPhantomReference classRef, int line, ITraceNode caller) {
		f_class  = classRef;
		f_line   = line;
		f_caller = caller;
	}
	
	public TraceNode getNode() {
		// First, try to see if I've cached a matching TraceNode
		TraceNode caller;
		ITraceNode callee;
		if (f_caller != null) {			
			// There's already a caller
			caller = f_caller.getNode();			
			callee = caller.getCallee(this);
		} else {
			// No caller yet
			caller = null;
			synchronized (TraceNode.roots) {	
				callee = TraceNode.roots.get(this);	
			}
		}
		if (callee != null) {
			return callee.getNode();
		}		
		return TraceNode.newTraceNode(caller, f_class, f_line, Store.getRawQueue());
	}
	
	public ITraceNode getCallee(ICallLocation key) {
		return null;
	}
	
	public ITraceNode getParent() {
		return f_caller;
	}
	
	public final int getLine() {
		return f_line;
	}

	public final long getWithinClassId() {
		return f_class.getId();
	}
	
	@Override
	public int hashCode() {
		return (int) (f_class.getId() + f_line);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ICallLocation) {
			ICallLocation bt = (ICallLocation) o;
			return bt.getLine() == f_line &&
			       bt.getWithinClassId() == f_class.getId();
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
