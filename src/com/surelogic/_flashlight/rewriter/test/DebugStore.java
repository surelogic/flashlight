package com.surelogic._flashlight.rewriter.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class DebugStore {
  public static synchronized void fieldAccess(final boolean read, final Object receiver,
      final Field field, Class<?> withinClass, final int line) {
    System.out.println("fieldAccess");
    System.out.println("  read = " + read);
    System.out.println("  receiver = " + receiver);
    System.out.println("  field = " + field);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void beforeIntrinsicLockAcquisition(final Object lockObject,
      final boolean lockIsThis, final boolean lockIsClass,
      Class<?> withinClass, final int line) {
    System.out.println("beforeIntrinsicLockAcquisition");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  lockIsThis = " + lockIsThis);
    System.out.println("  lockIsClass = " + lockIsClass);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void afterIntrinsicLockAcquisition(final Object lockObject,
      Class<?> withinClass, final int line) {
    System.out.println("afterIntrinsicLockAcquisition");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
  
  public static synchronized void afterIntrinsicLockRelease(final Object lockObject,
      Class<?> withinClass, final int line) {
    System.out.println("afterIntrinsicLockRelease");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void intrinsicLockWait(final boolean before,
      final Object lockObject, Class<?> withinClass, final int line) {
    System.out.println("intrinsicLockWait");
    System.out.println("  before = " + before);
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
  
  public static synchronized void methodCall(final boolean before, final Object receiver,
      final String enclosingFileName, final String enclosingLocationName,
      Class<?> withinClass, final int line) {
    System.out.println("methodCall");
    System.out.println("  before = " + before);
    System.out.println("  receiver = " + receiver);
    System.out.println("  enclosingFileName = " + enclosingFileName);
    System.out.println("  enclosingLocationName = " + enclosingLocationName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void constructorCall(final boolean before,
      final Constructor constructor, final String enclosingFileName,
      final String enclosingLocationName, Class<?> withinClass,
      final int line) {
    System.out.println("constructorCall");
    System.out.println("  before = " + before);
    System.out.println("  constructor = " + constructor);
    System.out.println("  enclosingFileName = " + enclosingFileName);
    System.out.println("  enclosingLocationName = " + enclosingLocationName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
  
  public static synchronized void constructorExecution(final boolean before,
      final Object receiver, Class<?> withinClass, final int line) {
    System.out.println("constructorExecution");
    System.out.println("  before = " + before);
    System.out.println("  receiver = " + receiver);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void beforeUtilConcurrentLockAcquisitionAttempt(
      final Object lockObject, Class<?> withinClass, final int line) {
    System.out.println("beforeUtilConcurrentLockAcquisitionAttempt");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void afterUtilConcurrentLockAcquisitionAttempt(
      final boolean gotTheLock, final Object lockObject,
      Class<?> withinClass, final int line) {
    System.out.println("afterUtilConcurrentLockAcquisitionAttempt");
    System.out.println("  gotTheLock = " + gotTheLock);
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void afterUtilConcurrentLockReleaseAttempt(
      final boolean releasedTheLock, final Object lockObject,
      Class<?> withinClass, final int line) {
    System.out.println("afterUtilConcurrentLockReleaseAttempt");
    System.out.println("  releasedTheLock = " + releasedTheLock);
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
}
