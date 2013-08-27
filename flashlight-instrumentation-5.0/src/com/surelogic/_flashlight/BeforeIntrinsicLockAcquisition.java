package com.surelogic._flashlight;

final class BeforeIntrinsicLockAcquisition extends IntrinsicLock {

    BeforeIntrinsicLockAcquisition(final Object lockObject,
            final boolean lockIsThis, final long siteId,
            final PostMortemStore.State state) {
        super(lockObject, siteId, state, lockIsThis);

    }

    @Override
    void accept(final EventVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("<before-intrinsic-lock-acquisition");
        addNanoTime(b);
        addThread(b);
        addLock(b);
        if (isLockThis()) {
            Entities.addAttribute("lock-is-this", "yes", b);
        }
        b.append("/>");
        return b.toString();
    }
}
