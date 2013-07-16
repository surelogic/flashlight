package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBeforeObject extends HappensBeforeEvent {

    private final long obj;
    private final long siteId;

    HappensBeforeObject(final String id, final ObjectPhantomReference obj,
            long siteId, State state, long nanoTime) {
        super(id, siteId, state, nanoTime);
        this.obj = obj.getId();
        this.siteId = siteId;
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
        addId(b);
        addNanoTime(b);
        addThread(b);
        Entities.addAttribute(AttributeType.SITE_ID.label(), siteId, b);
        Entities.addAttribute(AttributeType.OBJECT.label(), obj, b);
        b.append("/>");
        return b.toString();
    }
}
