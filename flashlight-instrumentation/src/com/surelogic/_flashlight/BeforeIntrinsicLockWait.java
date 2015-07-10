package com.surelogic._flashlight;

final class BeforeIntrinsicLockWait extends IntrinsicLock {

  BeforeIntrinsicLockWait(final Object lockObject, final long siteId, final PostMortemStore.State state, boolean lockIsThis) {
    super(lockObject, siteId, state, lockIsThis);
  }

  @Override
  void accept(final EventVisitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("<before-intrinsic-lock-wait");
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
