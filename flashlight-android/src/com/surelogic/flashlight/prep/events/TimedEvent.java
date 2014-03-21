package com.surelogic.flashlight.prep.events;

abstract class TimedEvent {
    protected final long nanoTime;
    protected final long inThread;

    TimedEvent(long nanos, long thread) {
        nanoTime = nanos;
        inThread = thread;
    }

    public long getNanoTime() {
        return nanoTime;
    }

}
