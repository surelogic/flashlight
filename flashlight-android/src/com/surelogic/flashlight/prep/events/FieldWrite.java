package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class FieldWrite extends FieldAccess implements Event {

    FieldWrite(long field, long nanos, long thread, long trace) {
        super(field, nanos, thread, trace);

    }

    @Override
    public PrepEvent type() {
        return PrepEvent.FIELDWRITE;
    }

    @Override
    public boolean isRead() {
        return false;
    }

}
