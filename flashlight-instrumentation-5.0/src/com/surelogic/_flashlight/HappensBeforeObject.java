package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBeforeObject extends TracedEvent {

    private final long obj;

    HappensBeforeObject(final ObjectPhantomReference obj, long siteId,
            State state) {
        super(siteId, state);
        this.obj = obj.getId();
    }

    @Override
    void accept(EventVisitor v) {
        v.visit(this);
    }

    public long getObj() {
        return obj;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(128);
        b.append("<happens-before-obj");
        addNanoTime(b);
        addThread(b);
        addObj(b);
        b.append("/>");
        return b.toString();
    }

    private void addObj(StringBuilder b) {
        Entities.addAttribute(AttributeType.OBJECT.label(), obj, b);
    }
}
