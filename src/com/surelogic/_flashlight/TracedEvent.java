package com.surelogic._flashlight;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.trace.*;

public abstract class TracedEvent extends WithinThreadEvent {
	private final TraceNode trace; // = TraceNode.getCurrentNode();
	
	TracedEvent(final long siteId, final Store.State state) {		
		super(state.thread);
		/*
		if (trace == null) {
			System.out.println("??? -> "+withinClass.getName()+":"+line);		
		} else {
			System.out.println(trace.getWithinClassName()+":"+trace.getLine()+" -> "+
					           withinClass.getName()+":"+line);		 
		}
		*/
		if (siteId == IdConstants.SYNTHETIC_METHOD_SITE_ID) {
			trace = state.getCurrentTrace();
		} else {
			trace = state.getCurrentTrace(siteId);
		}
	}
	
	long getTraceId() {
		return trace == null ? 0 : trace.getId();
	}
	
	@Override
	protected final void addThread(final StringBuilder b) {
		super.addThread(b);
		Entities.addAttribute(AttributeType.TRACE.label(), getTraceId(), b);
	}
}
