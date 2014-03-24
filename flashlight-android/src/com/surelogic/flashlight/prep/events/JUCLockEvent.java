package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class JUCLockEvent extends LockEvent {

    JUCLockEvent(PrepEvent event, long nanos, long thread, long trace,
            long lock, boolean isSuccess) {
        super(event, nanos, thread, trace, lock, isSuccess);
    }

    public JUCLockEvent(PrepEvent event, long nanos, long thread, long trace,
            long lock) {
        super(event, nanos, thread, trace, lock);
    }

}
