package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBeforeObject extends TracedEvent {

    private final boolean isSource;
    private final long obj;

    HappensBeforeObject(final ObjectPhantomReference obj, long siteId,
            State state, boolean isSource) {
        super(siteId, state);
        this.isSource = isSource;
        this.obj = obj.getId();
    }

    @Override
    void accept(EventVisitor v) {
        v.visit(this);
    }

    public boolean isSource() {
        return isSource;
    }

    public long getObj() {
        return obj;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(128);
        if (isSource) {
            b.append("<happens-before-obj");
        } else {
            b.append("<happens-before-obj");
        }
        addNanoTime(b);
        addThread(b);
        addColl(b);
        b.append("/>");
        return b.toString();
    }

    private void addColl(StringBuilder b) {
        Entities.addAttribute(AttributeType.OBJECT.label(), obj, b);
        Entities.addAttribute(AttributeType.ISSOURCE.label(), isSource, b);
    }
}
