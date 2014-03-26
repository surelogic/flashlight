package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.LockId;
import com.surelogic.flashlight.common.LockType;
import com.surelogic.flashlight.common.prep.PrepEvent;

public abstract class LockEvent extends TracedEvent implements Event {

    protected final long lock;
    protected final boolean success;

    private final PrepEvent type;

    protected LockEvent(PrepEvent event, long nanos, long thread, long trace,
            long lock, boolean isSuccess) {
        super(nanos, thread, trace);
        type = event;
        this.lock = lock;
        success = isSuccess;
    }

    protected LockEvent(PrepEvent event, long nanos, long thread, long trace,
            long lock) {
        super(nanos, thread, trace);
        type = event;
        this.lock = lock;
        success = true;
    }

    @Override
    public PrepEvent getEventType() {
        return type;
    }

    public LockId getLockId() {
        LockType lt;
        switch (type) {
        case BEFOREINTRINSICLOCKACQUISITION:
        case BEFOREINTRINSICLOCKWAIT:
        case AFTERINTRINSICLOCKACQUISITION:
        case AFTERINTRINSICLOCKRELEASE:
        case AFTERINTRINSICLOCKWAIT:
            lt = LockType.INTRINSIC;
            break;
        default:
            lt = LockType.UTIL;
        }
        return new LockId(lock, lt);
    }

    public boolean isSuccess() {
        return success;
    }

}
