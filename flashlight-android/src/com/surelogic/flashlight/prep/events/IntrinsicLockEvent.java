package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class IntrinsicLockEvent extends LockEvent {

    private final boolean lockIsClass;
    private final boolean lockIsThis;

    IntrinsicLockEvent(PrepEvent event, long nanos, long thread, long trace,
            long lock, boolean lockIsThis, boolean lockIsClass) {
        super(event, nanos, thread, trace, lock);
        this.lockIsThis = lockIsThis;
        this.lockIsClass = lockIsClass;
    }

    public boolean isLockIsClass() {
        return lockIsClass;
    }

    public boolean isLockIsThis() {
        return lockIsThis;
    }

}
