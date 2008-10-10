package com.surelogic._flashlight.trace;

import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.*;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.LongMap;

public abstract class TraceNode extends AbstractCallLocation implements ITraceNode {	
	public static final boolean inUse = IdConstants.useTraceNodes;
	static final boolean recordOnPush = false;
	
	/**
	 * This represents the top 48 bits of the id
	 */
	private static final AtomicLong nextSequenceId = new AtomicLong(0); 
	private static final int SEQUENCE_SHIFT = 16;
	private static final int SEQUENCE_MASK  = (1 << SEQUENCE_SHIFT) - 1;
	
	static final LongMap<TraceNode> roots = new LongMap<TraceNode>();
	//static int poppedTotal = 0, poppedPlaceHolders = 0;
	
	//private final long f_id = nextId.getAndIncrement();
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
    
	private TraceNode(TraceNode caller, final long siteId) {
	    super(siteId);
		f_caller  = caller;
	}
	
	static class IntVersion extends TraceNode {
		private final int f_id;
		
		IntVersion(TraceNode caller, final long siteId, int id) {
		    super(caller, siteId);
			f_id = id;
		}
		@Override
		public long getId() {
			return f_id;
		}
	}
	static class LongVersion extends TraceNode {
		private final long f_id;
		
		LongVersion(TraceNode caller, final long siteId, long id) {
		    super(caller, siteId);
			f_id = id;
		}
		@Override
		public long getId() {
			return f_id;
		}
	}
	
	private static TraceNode newTraceNode(final Header header, final TraceNode caller, 
			                              final long siteId) {
		final long id  = header.getNextId();
		final int cast = (int) id;
		if (cast == id) {
			return new IntVersion(caller, siteId, cast);
		}
		return new LongVersion(caller, siteId, id);
	}
	
	static TraceNode newTraceNode(final TraceNode caller, final long siteId, 
			                      Store.State state) {
		TraceNode callee = newTraceNode(state.traceHeader, caller, siteId);
		TraceNode firstCallee;				
		if (caller != null) {
			// Insert into caller
			synchronized (caller) {
				if (caller.f_calleeNodes == null) {
					firstCallee = null; 
				} else {
					firstCallee = (TraceNode) caller.getCallee(callee.getSiteId());
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
			    Store.putInQueue(state, callee);
			}
		} else {
			// Insert into roots
			synchronized (roots) {
				firstCallee = roots.get(callee.getSiteId());
				if (firstCallee == null) {
					roots.put(callee.getSiteId(), callee);
				} else {
					callee = firstCallee;
				}
				//callee.unpropagated++;
			}
			if (firstCallee == null) {
				Store.putInQueue(state, callee);
			}
		}
		return callee;
	}
	
	public static void pushTraceNode(final long siteId, Store.State state) {
		final Header header     = state.traceHeader;
		final ITraceNode caller = header.current;
		ITraceNode callee = null;
		if (caller != null) {
			// There's already a caller
			callee = caller.getCallee(siteId);
			if (callee == null) {
				// Try to insert a new TraceNode
				if (recordOnPush) {
					callee = newTraceNode(caller.getNode(state), siteId, state);
				} else {
					callee = caller.pushCallee(siteId);
				}
			}
		} else {			
			// No caller yet
			synchronized (roots) {	
				callee  = roots.get(siteId);				
			    if (callee == null) {
					if (recordOnPush) {
						callee = newTraceNode(null, siteId, state);
					} else {
						callee = new Placeholder(caller /*null*/, siteId);
					}	
			    }
			}
		}		
		header.current = callee;
		header.count++;
	}
	
	public static void popTraceNode(final long siteId, Store.State state) {	
		final Header header     = state.traceHeader;
		final ITraceNode callee = header.current;
		if (callee != null) {
			ITraceNode parent = callee.popParent();
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
	
	public abstract long getId();
	
	public final long getParentId() {
	    return f_caller == null ? 0 : f_caller.getId();
	}
	
	@Override
	protected void accept(EventVisitor v) {
	    v.visit(this);
	}
	
	public final ITraceNode pushCallee(long siteId) {
		return new Placeholder(this, siteId);
	}
	
	public final ITraceNode popParent() {
		return f_caller;
	}
	
	public final ITraceNode peekParent() {
		return f_caller;
	}
	
	public TraceNode getNode(Store.State state) {
		return this;
	}
	
	public synchronized ITraceNode getCallee(long key) {
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
	private static TraceNode findCallee(final TraceNode root, final long key) {
		TraceNode here   = root;
		TraceNode callee = root.f_calleeNodes;
		while (callee != null) {
			/*
			if (callee.peekParent() != root) {
				System.out.println("Parent doesn't match");
			}
			*/
			if (key == callee.getSiteId()) {
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
	
	static long getFirstIdInSequence() {
		long top48 = nextSequenceId.getAndIncrement();
		long id;
		if (top48 == 0) {
			id = 1; // 0 is for no parent (null)
		} else {
			id = top48 << SEQUENCE_SHIFT;
		}
		return id;
	}
	
	public static Header makeHeader() {
		return new Header();
	}
	
	/**
	 * A thread-local class
	 * Helps to keep stats, as well as avoid calls to ThreadLocal.set()
	 */	
	public static class Header implements IThreadState {
		int count = 0;
		long nextId = getFirstIdInSequence(); 
		ITraceNode current = null;
		
		Header() {
			// Nothing to do
		}
		
		long getNextId() {
			long id = nextId;
			if ((id & SEQUENCE_MASK) == SEQUENCE_MASK) { 
				// last id in sequence
				nextId = getFirstIdInSequence();
			} else {
				nextId++;
			}
			return id;
		}

		public TraceNode getCurrentNode(Store.State state) {
			final ITraceNode current = this.current;
			if (current == null) {
				return null;
			}
			TraceNode real = current.getNode(state); 
			if (real != current) {
				// Remove placeholders if there are any
				this.current = real;
			}
			return real;
		}
	}
	
	void pruneTree() {
		System.err.println("Pruning ...");
		// FIX What to do instead?
		synchronized (this) {
			f_calleeNodes = null;
			f_siblingNodes = null;
		}
		TraceNode here = f_caller;
		while (here != null) {
			synchronized (here) {
				here.f_siblingNodes = null;
				here = here.f_caller;
			}
		}
	}
}
