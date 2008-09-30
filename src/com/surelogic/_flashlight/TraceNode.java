package com.surelogic._flashlight;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.common.IdConstants;

public class TraceNode extends AbstractCallLocation {
	static final boolean inUse = IdConstants.useTraceNodes;
	private static final AtomicLong nextId = new AtomicLong(1); // 0 is for no parent (null)
	private static final ThreadLocal<TraceNode> currentNode = new ThreadLocal<TraceNode>();
	private static final Map<ICallLocation,TraceNode> roots = new HashMap<ICallLocation,TraceNode>();
	
	final long f_id = nextId.getAndIncrement();
	final TraceNode f_caller;
	final ConcurrentMap<ICallLocation,TraceNode> calleeNodes = new ConcurrentHashMap<ICallLocation, TraceNode>(4);	
	
	private TraceNode(TraceNode caller, ClassPhantomReference inClass, int line) {
	    super(inClass, line);
		f_caller  = caller;
	}
	
	static TraceNode pushTraceNode(ClassPhantomReference inClass, int line, BlockingQueue<List<Event>> queue) {
		final TraceNode caller = currentNode.get();
		TraceNode callee = null;
		if (caller != null) {
			// There's already a caller
			Key key          = new Key(inClass.getId(), line);
			 callee = caller.calleeNodes.get(key);
			if (callee == null) {
				// Try to insert a new TraceNode
				callee = new TraceNode(caller, inClass, line);
				TraceNode firstCallee = caller.calleeNodes.putIfAbsent(callee, callee);
				if (firstCallee != null) {
					// Already present, so use that one
					callee = firstCallee;
				} else {
				    Store.putInQueue(queue, callee);
				}
			}
		} else {
			// No caller yet
			synchronized (roots) {	
				/*
			    for (TraceNode root : roots) {
			        if (root.getWithinClassId() == inClass.getId() && root.getLine() == line) {
			            callee = root;
			            break;
			        }
			    }
			    */
				Key key = new Key(inClass.getId(), line);
				callee  = roots.get(key);				
			    if (callee == null) {
			        callee = new TraceNode(null, inClass, line);			
			        roots.put(callee, callee);
			        Store.putInQueue(queue, callee);
			    }
			}
		}		
		currentNode.set(callee);
		return callee;
	}
	
	static TraceNode popTraceNode(long classId, int line) {
		final TraceNode callee = currentNode.get();
		if (callee != null) {
			currentNode.set(callee.f_caller);
		}
		return callee;
	}
	
	static TraceNode getCurrentNode() {
		return currentNode.get();
	}
	
	/*
	static TraceNode ensureStackTrace(ClassPhantomReference inClass, int line) {
	    // Make sure the current trace matches the real trace
	    // Note: top and bottom of the trace might not match?	    
	    Throwable forTrace = new Throwable();
	    // Uses names to id
	    // -- not enough info to recreate the stack
	    StackTraceElement[] stack = forTrace.getStackTrace();

	    return null; // FIX
	}
	*/	
	
	public final long getId() {
		return f_id;
	}
	
	public final long getParentId() {
	    return f_caller == null ? 0 : f_caller.getId();
	}
	
	@Override
	void accept(EventVisitor v) {
	    v.visit(this);
	}
	
	static class Key implements ICallLocation {
		final long f_classId;
		final int f_line;
		
		Key(long classId, int line) {
			f_classId = classId;
			f_line    = line;
		}
		
		public final int getLine() {
			return f_line;
		}

		public final long getWithinClassId() {
			return f_classId;
		}
		
		@Override
		public int hashCode() {
			return (int) (f_classId + f_line);
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof ICallLocation) {
				ICallLocation bt = (ICallLocation) o;
				return bt.getLine() == f_line &&
				       bt.getWithinClassId() == f_classId;
	 		}
			return false;
		}
	}
}
