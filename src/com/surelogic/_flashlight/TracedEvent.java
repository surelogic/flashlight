package com.surelogic._flashlight;

import com.surelogic._flashlight.common.AttributeType;

public abstract class TracedEvent extends WithinThreadEvent {
	private final TraceNode trace = TraceNode.getCurrentNode();
	
	TracedEvent(final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line);
		/*
		if (trace == null) {
			System.out.println("??? -> "+withinClass.getName()+":"+line);		
		} else {
			System.out.println(trace.getWithinClassName()+":"+trace.getLine()+" -> "+
					           withinClass.getName()+":"+line);		 
		}
		*/
	}
	
	long getTraceId() {
		return trace == null ? 0 : trace.getId();
	}
	
	@Override
	protected final void addThread(final StringBuilder b) {
		super.addThread(b);
		if (TraceNode.inUse) {
			Entities.addAttribute(AttributeType.TRACE.label(), trace.getId(), b);
		}
	}
}
