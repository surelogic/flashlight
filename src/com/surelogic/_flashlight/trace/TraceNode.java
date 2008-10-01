package com.surelogic._flashlight.trace;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.*;
import com.surelogic._flashlight.common.IdConstants;

public class TraceNode extends AbstractCallLocation implements ITraceNode {	
	public static final boolean inUse = IdConstants.useTraceNodes;
	static final boolean recordOnPush = false;
	private static final AtomicLong nextId = new AtomicLong(1); // 0 is for no parent (null)
	private static final TraceThreadLocal currentNode = new TraceThreadLocal();
	static final Map<ICallLocation,TraceNode> roots = new HashMap<ICallLocation,TraceNode>();
	
	private final long f_id = nextId.getAndIncrement();
	private final TraceNode f_caller;
	/*
	private final ConcurrentMap<ICallLocation,TraceNode> calleeNodes = 
		new ConcurrentHashMap<ICallLocation, TraceNode>(4, 0.75f, 2);	

	private Map<ICallLocation,TraceNode> calleeNodes = null;
	//	new HashMap<ICallLocation, TraceNode>(0);	
	*/
	private List<TraceNode> calleeNodes = null;
	
	TraceNode(TraceNode caller, ClassPhantomReference inClass, int line) {
	    super(inClass, line);
		f_caller  = caller;

	}
	
	static TraceNode newTraceNode(TraceNode caller, ClassPhantomReference inClass, int line, 
			                      BlockingQueue<List<Event>> queue) {
		TraceNode callee = new TraceNode(caller, inClass, line);
				
		if (caller != null) {
			// Insert into caller
			TraceNode firstCallee;
			//firstCallee = caller.calleeNodes.putIfAbsent(callee, callee);
			synchronized (caller) {
				int i;
				if (caller.calleeNodes == null) {
					//caller.calleeNodes = new HashMap<ICallLocation, TraceNode>(1);
					caller.calleeNodes = new ArrayList<TraceNode>(2); 
					i = -1;
				} else {
					i = caller.calleeNodes.indexOf(callee);
				}
				//firstCallee = caller.calleeNodes.put(callee, callee);
				if (i < 0) {
					caller.calleeNodes.add(callee);
					firstCallee = null; 
				} else {
					firstCallee = caller.calleeNodes.get(i);
				}
			}
			if (firstCallee != null) {
				// Already present, so use that one
				callee = firstCallee;
			} 
			else {
			    Store.putInQueue(queue, callee);
			}
		} else {
			// Insert into roots
			synchronized (roots) {							
				roots.put(callee, callee);
			}
	        Store.putInQueue(queue, callee);
		}
		return callee;
	}
	
	public static void pushTraceNode(ClassPhantomReference inClass, int line, BlockingQueue<List<Event>> queue) {
		final ITraceNode caller = currentNode.get();
		final Placeholder key   = new Placeholder(inClass, line, caller);
		ITraceNode callee = null;
		if (caller != null) {
			// There's already a caller
			callee = caller.getCallee(key);
			if (callee == null) {
				// Try to insert a new TraceNode
				callee = recordOnPush ? 
						 newTraceNode(caller.getNode(), inClass, line, queue) : key;
			}
		} else {			
			// No caller yet
			synchronized (roots) {	
				callee  = roots.get(key);				
			    if (callee == null) {
					callee = recordOnPush ? 
							 newTraceNode(null, inClass, line, queue) : key;		
			    }
			}
		}		
		currentNode.set(callee);
	}
	
	public static void popTraceNode(long classId, int line) {		
		final ITraceNode callee = currentNode.get();
		if (callee != null) {
			currentNode.set(callee.getParent());
		}
	}
	
	public static TraceNode getCurrentNode() {
		ITraceNode current = currentNode.get();
		if (current == null) {
			return null;
		}
		TraceNode real = current.getNode(); 
		if (real != current) {
			// Remove placeholders if there are any
			currentNode.set(real);
		}
		return real;
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
	protected void accept(EventVisitor v) {
	    v.visit(this);
	}
	
	public final ITraceNode getParent() {
		return f_caller;
	}
	
	public TraceNode getNode() {
		return this;
	}
	
	public synchronized ITraceNode getCallee(ICallLocation key) {
		if (calleeNodes == null) {
			return null;
		}
		//return calleeNodes.get(key);	
		int i = calleeNodes.indexOf(key);
		return i < 0 ? null : calleeNodes.get(i);
	}
}
