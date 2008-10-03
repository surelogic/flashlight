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
	private static final ThreadLocal<Header> currentNode = new ThreadLocal<Header>() {
		@Override
		protected Header initialValue() {
			return new Header();
		}
	};
	static final Map<ICallLocation,TraceNode> roots = new HashMap<ICallLocation,TraceNode>();
	//static int poppedTotal = 0, poppedPlaceHolders = 0;
	
	private final long f_id = nextId.getAndIncrement();
	private final TraceNode f_caller;
	/*
	private final ConcurrentMap<ICallLocation,TraceNode> calleeNodes = 
		new ConcurrentHashMap<ICallLocation, TraceNode>(4, 0.75f, 2);	

	private Map<ICallLocation,TraceNode> calleeNodes = null;
	//	new HashMap<ICallLocation, TraceNode>(0);	

	private List<TraceNode> calleeNodes = null;
    */
    /**
     * Same caller as this
     */	
	private TraceNode f_siblingNodes = null;
	/**
	 * Have this as the caller
	 */
	private TraceNode f_calleeNodes = null;
	/*
	private int count = 0;
	private int unpropagated = 0; // To parent
	
	public int getAndClearUnpropagated() {
		int rv = unpropagated;
		unpropagated = 0;
		count += rv;
		
		//if ((count & 0x7ff) == 0) {
		//	System.err.println(count);
		//}		
		return rv;
	}
	
	public int addToUnpropagated(int add) {
		return (unpropagated += add);	
	}
    */
    
	private TraceNode(TraceNode caller, ClassPhantomReference inClass, int line) {
	    super(inClass, line);
		f_caller  = caller;

	}
	
	static TraceNode newTraceNode(final TraceNode caller, ClassPhantomReference inClass, int line, 
			                      BlockingQueue<List<Event>> queue) {
		TraceNode callee = new TraceNode(caller, inClass, line);
		TraceNode firstCallee;				
		if (caller != null) {
			// Insert into caller

			//firstCallee = caller.calleeNodes.putIfAbsent(callee, callee);
			synchronized (caller) {
				/*
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
				*/
				if (caller.f_calleeNodes == null) {
					firstCallee = null; 
				} else {
					firstCallee = (TraceNode) caller.getCallee(callee);
					/*
					if (firstCallee != null && caller != firstCallee.getParent()) {
						System.out.println("Parent doesn't match");
					}
					*/
				}
				//callee.unpropagated++;
			}
			if (firstCallee != null) {
				// Already present, so use that one
				callee = firstCallee;
				//callee.unpropagated++;
			} 
			else {
				callee.f_siblingNodes = caller.f_calleeNodes;
				caller.f_calleeNodes  = callee;
			    Store.putInQueue(queue, callee);
			}
		} else {
			// Insert into roots
			synchronized (roots) {
				firstCallee = roots.get(callee);
				if (firstCallee == null) {
					roots.put(callee, callee);
				} else {
					callee = firstCallee;
				}
				//callee.unpropagated++;
			}
			if (firstCallee == null) {
				Store.putInQueue(queue, callee);
			}
		}
		return callee;
	}
	
	public static void pushTraceNode(ClassPhantomReference inClass, int line, BlockingQueue<List<Event>> queue) {
		final Header header     = currentNode.get();
		final ITraceNode caller = header.current;
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
		header.current = callee;
		header.count++;
	}
	
	public static void popTraceNode(long classId, int line) {	
		final Header header     = currentNode.get();
		final ITraceNode callee = header.current;
		if (callee != null) {
			ITraceNode parent = callee.getParent();
			header.current = parent;
			/*
			if (callee instanceof Placeholder) {
				poppedPlaceHolders++;
			}
			*/
			if (parent != null && parent instanceof TraceNode) {
				synchronized (parent) {
					/*
					int unprop  = callee.getAndClearUnpropagated();
					if (unprop != 0) {
						int parentU = parent.addToUnpropagated(unprop);
						if (parentU > 1000) {
							System.err.println(unprop+" -> "+parentU+"\t@ "+header.count);
						}			
					}
					*/
					if (header.count > 10000000) {
						header.count = 0;
						((TraceNode) parent).pruneTree();
					}
				}
			}
		}
		/*
		poppedTotal++;
		if ((poppedTotal & 0xfff) == 0) {
			System.err.println("Popped placeholders = "+poppedPlaceHolders+" out of "+poppedTotal);
		}
		*/
	}
	
	public static TraceNode getCurrentNode() {
		final Header header     = currentNode.get();
		final ITraceNode current = header.current;
		if (current == null) {
			return null;
		}
		TraceNode real = current.getNode(); 
		if (real != current) {
			// Remove placeholders if there are any
			header.current = real;
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
		if (f_calleeNodes == null) {
			return null;
		}
		/*
		//return calleeNodes.get(key);	
		int i = calleeNodes.indexOf(key);
		return i < 0 ? null : calleeNodes.get(i);
		*/
		//try {
			return findCallee(this, key);
		/*
		} catch (StackOverflowError e) {
			
			TraceNode here = this;
			while (here != null) {
				System.out.println("StackOverflowError: "+here.superToString());
				here = here.f_calleeNodes;
			}
			return null;
		}
		*/
	}
	
	/**
	 * @param root non-null
	 * @param key non-null	 
	 */
	private static TraceNode findCallee(final TraceNode root, final ICallLocation key) {
		TraceNode here   = root;
		TraceNode callee = root.f_calleeNodes;
		while (callee != null) {
			if (callee.getParent() != root) {
				System.out.println("Parent doesn't match");
			}
			if (key.equals(callee)) {
				if (here != root) {
					// Not the first node, so reorder the list
					// 1. Remove the callee node
					here.f_siblingNodes = callee.f_siblingNodes;
					// 2. Point the callee to match the root
					callee.f_siblingNodes = root.f_calleeNodes;
					// 3. Change the root to point to the callee
					root.f_calleeNodes = callee;
				}
				return callee;
			}
			/*
			if (here == callee) {
				System.out.println("Loop");
			}
			System.out.println("findCallee: "+callee.hashCode());			
			 */
			here   = callee;
			callee = here.f_siblingNodes;
		}
		return null;
	}
	
	/**
	 * Helps to keep stats, as well as avoid calls to ThreadLocal.set()
	 */	
	static class Header {
		int count = 0;
		ITraceNode current = null;
	}
	
	synchronized void pruneTree() {
		// FIX What to do instead?
		f_calleeNodes = null;
		f_siblingNodes = null;		
	}
}
