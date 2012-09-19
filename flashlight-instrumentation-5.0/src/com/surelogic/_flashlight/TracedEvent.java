package com.surelogic._flashlight;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.trace.TraceNode;

public abstract class TracedEvent extends WithinThreadEvent {
    private final TraceNode trace; // = TraceNode.getCurrentNode();

    TracedEvent(final long siteId, final PostMortemStore.State state) {
        super(state.thread);
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
