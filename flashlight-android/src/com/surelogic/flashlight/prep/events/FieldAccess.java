package com.surelogic.flashlight.prep.events;

public abstract class FieldAccess extends TracedEvent {

    private final long field;

    FieldAccess(long field, long nanos, long thread, long trace) {
        super(nanos, thread, trace);
        this.field = field;
    }

    public abstract boolean isRead();

    public long getField() {
        return field;
    }

}
