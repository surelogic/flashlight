package com.surelogic._flashlight.rewriter.test;

import com.surelogic._flashlight.ClassPhantomReference;
import com.surelogic._flashlight.ObjectPhantomReference;
import com.surelogic._flashlight.Phantom;

public class EmptyStore {
  /**
   * Get the phantom object reference for the given {@code Class} object.
   * Cannot use {@link Phantom#ofClass(Class)} directly because we need to make
   * sure the store is loaded and initialized before creating phantom objects.
   */
  public static ClassPhantomReference getClassPhantom(Class<?> c) {
    return Phantom.ofClass(c);
  }
  
  public static ObjectPhantomReference getObjectPhantom(Object o, long id) {
    return Phantom.ofObject(o, id);
  }

  public static void instanceFieldAccess(
      final boolean read, final Object receiver, final int fieldID,
      final long siteId) {
    // do nothing
  }

  public static void staticFieldAccess(final boolean read,
      final ClassPhantomReference fieldClass, final int fieldID,
      final long siteId) {
    // do nothing
  }

  public static void instanceFieldAccessLookup(
      final boolean read, final Object receiver,
      final Class clazz, final String fieldName,
      final long siteId) {
    // do nothing
  }

  public static void staticFieldAccessLookup(final boolean read,
      final Class clazz, final String fieldName,
      final long siteId) {
    // do nothing
  }

  public static void beforeIntrinsicLockAcquisition(final Object lockObject,
      final boolean lockIsThis, final boolean lockIsClass,
      final long siteId) {
    // do nothing
  }

  public static void afterIntrinsicLockAcquisition(
      final Object lockObject, final long siteId) {
    // do nothing
  }
  
  public static void afterIntrinsicLockRelease(
      final Object lockObject, final long siteId) {
    // do nothing
  }

  public static void intrinsicLockWait(
      final boolean before, final Object lockObject, final long siteId) {
    // do nothing
  }
  
  public static void methodCall(
      final boolean before, final Object receiver, final long siteid) {
    // do nothing
  }

  public static void constructorCall(final boolean before, final long siteId) { 
    // do nothing
  }
  
  public static void constructorExecution(
      final boolean before, final Object receiver, final long siteId) {
    // do nothing
  }

  public static void beforeUtilConcurrentLockAcquisitionAttempt(
      final Object lockObject, final long siteId) {
    // do nothing
  }

  public static void afterUtilConcurrentLockAcquisitionAttempt(
      final boolean gotTheLock, final Object lockObject,
      final long siteId) {
    // do nothing
  }

  public static void afterUtilConcurrentLockReleaseAttempt(
      final boolean releasedTheLock, final Object lockObject,
      final long siteId) {
    // do nothing
  }
}
