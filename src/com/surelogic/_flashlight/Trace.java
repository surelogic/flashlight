package com.surelogic._flashlight;

import com.surelogic._flashlight.trace.TraceNode;

abstract class Trace extends WithinThreadEvent {
	Trace(final ClassPhantomReference withinClass, final int line) {
		super(withinClass, line, TraceNode.getThreadState().getThread());
	}
}
