package com.surelogic.flashlight.prep.events;

public abstract class TracedEvent extends TimedEvent {

    protected final long trace;

    TracedEvent(long nanos, long thread, long trace) {
        super(nanos, thread);
        this.trace = trace;
    }

    public long getTrace() {
        return trace;
    }

}
