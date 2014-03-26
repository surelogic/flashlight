package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class TraceNode implements Event {
    private final long id;
    private final long parent;
    private final long site;

    public TraceNode(long id, long parent, long site) {
        super();
        this.id = id;
        this.parent = parent;
        this.site = site;
    }

    public long getId() {
        return id;
    }

    public long getParent() {
        return parent;
    }

    public long getSite() {
        return site;
    }

    @Override
    public PrepEvent getEventType() {
        return PrepEvent.TRACENODE;
    }
}
