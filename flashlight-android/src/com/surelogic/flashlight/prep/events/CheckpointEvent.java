package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class CheckpointEvent implements Event {

    private final long nanoTime;

    public CheckpointEvent(long nanoTime) {
        this.nanoTime = nanoTime;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    @Override
    public PrepEvent getEventType() {
        return PrepEvent.CHECKPOINT;
    }

}
