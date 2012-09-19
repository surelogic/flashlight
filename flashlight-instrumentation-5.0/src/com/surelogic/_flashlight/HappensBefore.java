package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBefore extends TracedEvent {

    private final boolean isFrom;
    private final long target;

    HappensBefore(final ObjectPhantomReference target, long siteId,
            State state, boolean isFrom) {
        super(siteId, state);
        this.isFrom = isFrom;
        this.target = target.getId();
    }

    @Override
    void accept(EventVisitor v) {
        v.visit(this);
    }

    public String getDirection() {
        return isFrom ? "from" : "to";
    }

    public long getTarget() {
        return target;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(128);
        b.append("<happens-before");
        addNanoTime(b);
        addThread(b);
        addTarget(b);
        addDirection(b);
        b.append("/>");
        return b.toString();
    }

    private void addDirection(StringBuilder b) {
        Entities.addAttribute(AttributeType.DIRECTION.label(), isFrom ? "from"
                : "to", b);
    }

    private void addTarget(StringBuilder b) {
        Entities.addAttribute(AttributeType.TARGET.label(), target, b);
    }
}
