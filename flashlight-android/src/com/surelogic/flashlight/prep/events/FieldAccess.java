package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class FieldAccess extends TracedEvent implements Event {

    private final long field;
    private final boolean read;
    private final PrepEvent event;
    private final Long receiver;

    FieldAccess(PrepEvent event, long field, long nanos, long thread,
            long trace, Long receiver) {
        super(nanos, thread, trace);
        this.field = field;
        read = event == PrepEvent.FIELDREAD;
        this.event = event;
        this.receiver = receiver;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isStatic() {
        return receiver == null;
    }

    public long getField() {
        return field;
    }

    @Override
    public PrepEvent getEventType() {
        return event;
    }

}
