package com.surelogic._flashlight;

public abstract class TracedEvent extends WithinThreadEvent {
	private final TraceNode trace = TraceNode.getCurrentNode();
	
	TracedEvent(final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line);
		if (trace == null) {
			System.out.println("??? -> "+withinClass.getName()+":"+line);		
		} else {
			System.out.println(trace.getWithinClassName()+":"+trace.getLine()+" -> "+
					           withinClass.getName()+":"+line);		 
		}
	}
}
