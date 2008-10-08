package com.surelogic._flashlight;

import com.surelogic._flashlight.trace.TraceNode;

abstract class Trace extends WithinThreadEvent {
	Trace(final long siteId) {
		super(siteId, TraceNode.getThreadState().getThread());
	}
}
