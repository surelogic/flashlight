package com.surelogic._flashlight.trace;

import static com.surelogic._flashlight.common.AttributeType.PARENT_ID;
import static com.surelogic._flashlight.common.AttributeType.SITE_ID;
import static com.surelogic._flashlight.common.AttributeType.TRACE;

import java.util.concurrent.atomic.AtomicLong;

import com.surelogic._flashlight.AbstractCallLocation;
import com.surelogic._flashlight.Entities;
import com.surelogic._flashlight.EventVisitor;

public class TraceNode extends AbstractCallLocation {

    private static final AtomicLong ID_SEQUENCE = new AtomicLong();

    private static long nextId() {
        return ID_SEQUENCE.incrementAndGet();
    }

    private final TraceNode parent;
    private final long id;

    protected TraceNode(TraceNode parent, long siteId) {
        super(siteId);
        this.parent = parent;
        id = nextId();
    }

    long getParentId() {
        return parent.getId();
    }

    long getId() {
        return id;
    }

    @Override
    protected void accept(EventVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("<").append("trace-node");
        Entities.addAttribute(TRACE.label(), getId(), b);
        Entities.addAttribute(SITE_ID.label(), getSiteId(), b);
        Entities.addAttribute(PARENT_ID.label(), getParentId(), b);
        b.append("/>");
        return b.toString();
    }

}
