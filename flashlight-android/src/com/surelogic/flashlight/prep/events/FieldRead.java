package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class FieldRead extends FieldAccess implements Event {
    FieldRead(long field, long nanos, long thread, long trace) {
        super(field, nanos, thread, trace);
    }

    @Override
    public PrepEvent type() {
        return PrepEvent.FIELDREAD;
    }

    @Override
    public boolean isRead() {
        return true;
    }
}
