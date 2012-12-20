package com.surelogic._flashlight;

import com.surelogic._flashlight.PostMortemStore.State;
import com.surelogic._flashlight.common.AttributeType;

public class HappensBeforeCollection extends TracedEvent {

    private final long obj;

    HappensBeforeCollection(final ObjectPhantomReference coll,
            final ObjectPhantomReference obj, long siteId, State state) {
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
        b.append("<happens-before-coll");
        addNanoTime(b);
        addThread(b);
        addColl(b);
        addObj(b);
        b.append("/>");
        return b.toString();
    }

    private void addColl(StringBuilder b) {
        Entities.addAttribute(AttributeType.COLLECTION.label(), obj, b);
    }

    private void addObj(StringBuilder b) {
        Entities.addAttribute(AttributeType.OBJECT.label(), obj, b);
    }

}
