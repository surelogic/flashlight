package com.surelogic._flashlight;

abstract class IntrinsicLock extends Lock {
  IntrinsicLock(final Object lockObject, final long siteId, final PostMortemStore.State state, final boolean lockIsThis) {
    super(lockObject, siteId, state);
    f_lockIsThis = lockIsThis;
  }

  /**
   * <code>true</code> if the lock object is dynamically the same as the
   * receiver object.
   */
  private final boolean f_lockIsThis;

  boolean isLockThis() {
    return f_lockIsThis;
  }
}
