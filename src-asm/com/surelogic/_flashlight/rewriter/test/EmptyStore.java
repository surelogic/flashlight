package com.surelogic._flashlight.rewriter.test;

import java.lang.reflect.Field;

import com.surelogic._flashlight.ClassPhantomReference;

public class EmptyStore {
  public static void instanceFieldAccess(
      final boolean read, final Object receiver, final int fieldID,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void staticFieldAccess(
      final boolean read, final int fieldID,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void instanceFieldAccessLookup(
      final boolean read, final Object receiver, final Field field,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void staticFieldAccessLookup(
      final boolean read, final Field field,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

//  public static void fieldAccess(final boolean read, final Object receiver,
//      final Field field, final ClassPhantomReference withinClass, final int line) {
//    // do nothing
//  }

  public static void beforeIntrinsicLockAcquisition(final Object lockObject,
      final boolean lockIsThis, final boolean lockIsClass,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void afterIntrinsicLockAcquisition(final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }
  
  public static void afterIntrinsicLockRelease(final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void intrinsicLockWait(final boolean before,
      final Object lockObject, final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }
  
  public static void methodCall(final boolean before, final Object receiver,
      final String enclosingFileName, final String enclosingLocationName,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void constructorCall(final boolean before,
      final String enclosingFileName,
      final String enclosingLocationName, final ClassPhantomReference withinClass,
      final int line) {
    // do nothing
  }
  
  public static void constructorExecution(final boolean before,
      final Object receiver, final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void beforeUtilConcurrentLockAcquisitionAttempt(
      final Object lockObject, final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void afterUtilConcurrentLockAcquisitionAttempt(
      final boolean gotTheLock, final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }

  public static void afterUtilConcurrentLockReleaseAttempt(
      final boolean releasedTheLock, final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    // do nothing
  }
}
