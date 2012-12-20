package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBefore extends TracedEvent {

    private final boolean isInFrom;
    private final long to;

    HappensBefore(final ObjectPhantomReference to, long siteId, State state,
            boolean isInFrom) {
        super(siteId, state);
        this.isInFrom = isInFrom;
        this.to = to.getId();
    }

    @Override
    void accept(EventVisitor v) {
        v.visit(this);
    }

    public long getSource() {
        if (isInFrom) {
            return getWithinThread().getId();
        } else {
            return to;
        }
    }

    public long getTarget() {
        if (isInFrom) {
            return to;
        } else {
            return getWithinThread().getId();
        }
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(128);
        b.append("<happens-before");
        addNanoTime(b);
        addThread(b);
        addEdge(b);
        b.append("/>");
        return b.toString();
    }

    private void addEdge(StringBuilder b) {
        Entities.addAttribute(AttributeType.SOURCE.label(), getSource(), b);
        Entities.addAttribute(AttributeType.TARGET.label(), getTarget(), b);
    }
}
