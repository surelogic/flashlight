package com.surelogic._flashlight;

public abstract class UtilConcurrentLock extends Lock {
  UtilConcurrentLock(final java.util.concurrent.locks.Lock lockObject, final long siteId, final PostMortemStore.State state) {
    super(lockObject, siteId, state);
  }
}
