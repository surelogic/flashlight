package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBeforeCollection extends HappensBeforeEvent {

    private final long coll;
    private final long obj;
    private final long siteId;

    HappensBeforeCollection(final ObjectPhantomReference coll,
            final ObjectPhantomReference obj, long siteId, State state,
            long nanoTime) {
        super(siteId, state, nanoTime);
        this.coll = coll.getId();
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
        b.append("<happens-before-coll");
        addNanoTime(b);
        addThread(b);
        Entities.addAttribute(AttributeType.SITE_ID.label(), siteId, b);
        Entities.addAttribute(AttributeType.COLLECTION.label(), coll, b);
        Entities.addAttribute(AttributeType.OBJECT.label(), obj, b);
        b.append("/>");
        return b.toString();
    }

}
