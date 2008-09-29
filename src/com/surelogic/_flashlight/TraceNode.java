package com.surelogic._flashlight;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TraceNode implements ICallLocation {
	private static final AtomicLong nextId = new AtomicLong(0);
	private static final ThreadLocal<TraceNode> currentNode = new ThreadLocal<TraceNode>();
	private static final List<TraceNode> roots = new ArrayList<TraceNode>();
	
	final long f_id = nextId.getAndIncrement();
	final long f_classId;
	final int f_line;
	final TraceNode f_caller;
	final ConcurrentMap<ICallLocation,TraceNode> calleeNodes = new ConcurrentHashMap<ICallLocation, TraceNode>(4);	
	
	private TraceNode(TraceNode caller, long classId, int line) {
		f_caller  = caller;
		f_classId = classId;
		f_line    = line;
	}
	
	static TraceNode pushTraceNode(long classId, int line) {
		final TraceNode caller = currentNode.get();
		TraceNode callee;
		if (caller != null) {
			// There's already a caller
			Key key          = new Key(classId, line);
			 callee = caller.calleeNodes.get(key);
			if (callee == null) {
				// Try to insert a new TraceNode
				callee = new TraceNode(caller, classId, line);
				TraceNode firstCallee = caller.calleeNodes.putIfAbsent(callee, callee);
				if (firstCallee != null) {
					// Already present, so use that one
					callee = firstCallee;
				}
			}
		} else {
			// No caller yet
			synchronized (roots) {	
				callee = new TraceNode(null, classId, line);			
				roots.add(callee);
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
	
	public final long getId() {
		return f_id;
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
