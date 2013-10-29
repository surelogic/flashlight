package com.surelogic.flashlight.common.prep;

import static com.surelogic._flashlight.common.FlagType.RELEASED_LOCK;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.LockType;

public final class AfterUtilConcurrentLockReleaseAttempt extends Lock {

    public AfterUtilConcurrentLockReleaseAttempt(
            final IntrinsicLockDurationRowInserter i) {
        super(i);
    }

    @Override
    protected LockType getType() {
        return LockType.UTIL;
    }

    @Override
    public String getXMLElementName() {
        return "after-util-concurrent-lock-release-attempt";
    }

    @Override
    protected LockState getState() {
        return LockState.AFTER_RELEASE;
    }

    @Override
    protected Boolean isSuccess(PreppedAttributes attr) {
        return attr.getBoolean(RELEASED_LOCK);
    }
}
