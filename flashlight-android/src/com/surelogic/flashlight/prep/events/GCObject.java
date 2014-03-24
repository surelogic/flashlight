package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class GCObject implements Event {
    private final long id;

    public GCObject(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public PrepEvent getEventType() {
        return PrepEvent.GARBAGECOLLECTEDOBJECT;
    }

}
