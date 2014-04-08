package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class TimeEvent implements Event {

    private final long nanoTime;
    private final String startTime;

    public TimeEvent(long nanoTime, String startTime) {
        this.nanoTime = nanoTime;
        this.startTime = startTime;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public String getStartTime() {
        return startTime;
    }

    @Override
    public PrepEvent getEventType() {
        return null;
    }

}
