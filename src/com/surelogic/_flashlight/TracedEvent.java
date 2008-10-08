package com.surelogic._flashlight;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.trace.IThreadState;
import com.surelogic._flashlight.trace.TraceNode;

public abstract class TracedEvent extends WithinThreadEvent {
	private final TraceNode trace; // = TraceNode.getCurrentNode();
	
	TracedEvent(final long siteId, final IThreadState state) {		
		super(siteId, state.getThread());
		/*
		if (trace == null) {
			System.out.println("??? -> "+withinClass.getName()+":"+line);		
		} else {
			System.out.println(trace.getWithinClassName()+":"+trace.getLine()+" -> "+
					           withinClass.getName()+":"+line);		 
		}
		*/
		trace = state.getCurrentNode();
	}
	
	long getTraceId() {
		return trace == null ? 0 : trace.getId();
	}
	
	@Override
	protected final void addThread(final StringBuilder b) {
		super.addThread(b);
		if (TraceNode.inUse) {
			Entities.addAttribute(AttributeType.TRACE.label(), getTraceId(), b);
		}
	}
}
