package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;

public abstract class HappensBeforeEvent extends ProgramEvent {

    private final long f_nanoTime;
    private final long f_threadId;
    private final long f_traceId;

    HappensBeforeEvent(long siteId, State state, long nanoTime) {
        f_threadId = state.thread.getId();
        f_nanoTime = nanoTime;
        if (siteId == IdConstants.SYNTHETIC_METHOD_SITE_ID) {
            f_traceId = state.getCurrentTrace().getId();
        } else {
            f_traceId = state.getCurrentTrace(siteId).getId();
        }
    }

    protected final void addNanoTime(final StringBuilder b) {
        Entities.addAttribute(AttributeType.TIME.label(), f_nanoTime, b);
    }

    protected void addThread(final StringBuilder b) {
        Entities.addAttribute(AttributeType.THREAD.label(), f_threadId, b);
        Entities.addAttribute(AttributeType.TRACE.label(), f_traceId, b);
    }
}
