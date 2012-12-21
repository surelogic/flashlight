package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBeforeThread extends TracedEvent {

    private final long to;
    private final long siteId;

    HappensBeforeThread(final ThreadPhantomReference to, long siteId,
            State state) {
        super(siteId, state);
        this.to = to.getId();
        this.siteId = siteId;
    }

    @Override
    void accept(EventVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(128);
        b.append("<happens-before-thread");
        addNanoTime(b);
        addThread(b);
        Entities.addAttribute(AttributeType.SITE_ID.label(), siteId, b);
        Entities.addAttribute(AttributeType.TOTHREAD.label(), to, b);
        b.append("/>");
        return b.toString();
    }

}
