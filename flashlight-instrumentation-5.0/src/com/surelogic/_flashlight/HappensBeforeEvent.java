package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;

public abstract class HappensBeforeEvent extends ProgramEvent {

    private final String f_id;
    private final long f_nanoStart;
    private final long f_nanoEnd;
    private final long f_threadId;
    private final long f_traceId;

    HappensBeforeEvent(String id, long siteId, State state, long nanoStart) {
        f_id = id;
        f_threadId = state.thread.getId();
        f_nanoStart = nanoStart;
        f_nanoEnd = System.nanoTime();
        if (siteId == IdConstants.SYNTHETIC_METHOD_SITE_ID) {
            f_traceId = state.getCurrentTrace().getId();
        } else {
            f_traceId = state.getCurrentTrace(siteId).getId();
        }
    }

    protected final void addId(final StringBuilder b) {
        Entities.addAttribute(AttributeType.ID.label(), f_id, b);
    }

    protected final void addNanoTime(final StringBuilder b) {
        Entities.addAttribute(AttributeType.NANO_START.label(), f_nanoStart, b);
        Entities.addAttribute(AttributeType.NANO_END.label(), f_nanoEnd, b);
    }

    protected void addThread(final StringBuilder b) {
        Entities.addAttribute(AttributeType.THREAD.label(), f_threadId, b);
        Entities.addAttribute(AttributeType.TRACE.label(), f_traceId, b);
    }
}
