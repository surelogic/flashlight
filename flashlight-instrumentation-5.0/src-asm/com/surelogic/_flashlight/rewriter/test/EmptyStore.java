package com.surelogic._flashlight.rewriter.test;

import com.surelogic._flashlight.ClassPhantomReference;
import com.surelogic._flashlight.ObjectPhantomReference;
import com.surelogic._flashlight.Phantom;
import com.surelogic._flashlight.StoreDelegate;

public class EmptyStore {
  public static void shutdown() {
    // do nothing
  }
  
  /**
   * Get the phantom object reference for the given {@code Class} object.
   * Cannot use {@link Phantom#ofClass(Class)} directly because we need to make
   * sure the store is loaded and initialized before creating phantom objects.
   */
  public static ClassPhantomReference getClassPhantom(Class<?> c) {
    return StoreDelegate.getClassPhantom(c);
  }
  
  public static ObjectPhantomReference getObjectPhantom(Object o, long id) {
    return StoreDelegate.getObjectPhantom(o, id);
  }

  public static void instanceFieldAccess(
      final boolean read, final Object receiver, final int fieldID,
      final long siteId, final ClassPhantomReference dcPhantom,
      final Class<?> declaringClass) {
    // do nothing
  }

  public static void staticFieldAccess(
      final boolean read, final int fieldID, final long siteId,
      final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
    // do nothing
  }

  public static void indirectAccess(
      final Object receiver, final long siteId) {
    // do nothing
  }
  public static void arrayAccess(
      final boolean read, final Object receiver, final int index, final long siteId) {
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

  public static void methodExecution(
      final boolean before, final long siteId) {
    // Do stuff
  }
  
  public static void methodCall(
      final boolean before, final Object receiver, final long siteid) {
    // do nothing
  }
  
  public static void classInit(final boolean before, final Class<?> clazz) {
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

  public static void instanceFieldInit(
      final Object receiver, final int fieldId, final Object value) {
    // do nothing
  }
  
  public static void staticFieldInit(final int fieldId, final Object value) {
    // do nothing
  }

  public static int getFieldId(final String clazz, final String field) {
    return -1;
  }

  public static void happensBeforeThread(
      final Thread callee, final long siteId, final String typeName) {
    // do nothing
  }
  
  public static void happensBeforeObject(
      final Object object, final long siteId, final String typeName) {
    // do nothing
  }
  
  public static void happensBeforeCollection(
      final Object collection, final Object item, final long siteId,
      final String typeName) {
    // do nothing
  }
}
