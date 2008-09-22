package com.surelogic._flashlight.rewriter.test;

import com.surelogic._flashlight.ClassPhantomReference;
import com.surelogic._flashlight.Phantom;

public class DebugStore {
  /**
   * Get the phantom object reference for the given {@code Class} object.
   * Cannot use {@link Phantom#ofClass(Class)} directly because we need to make
   * sure the store is loaded and initialized before creating phantom objects.
   */
  public static ClassPhantomReference getClassPhantom(Class<?> c) {
    return Phantom.ofClass(c);
  }

  /**
   * Use this instead of o.toString() because if toString() is overridden we can
   * end up having endless recursive calls to the the methodCall() event.
   */
  private static String objectToString(final Object o) {
    // From Object.toString();
    if (o == null) {
      return "null";
    } else {
      return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }
  }
  
  public static void instanceFieldAccess(
      final boolean read, final Object receiver, final int fieldID,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("instanceFieldAccess");
    System.out.println("  read = " + read);
    System.out.println("  receiver = " + objectToString(receiver));
    System.out.println("  fieldID = " + fieldID);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static void staticFieldAccess(final boolean read,
      final ClassPhantomReference fieldClass, final int fieldID,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("staticFieldAccess");
    System.out.println("  read = " + read);
    System.out.println("  fieldClass = " + fieldClass);
    System.out.println("  fieldID = " + fieldID);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static void instanceFieldAccessLookup(
      final boolean read, final Object receiver,
      final Class clazz, final String fieldName,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("instanceFieldAccessLookup");
    System.out.println("  read = " + read);
    System.out.println("  receiver = " + objectToString(receiver));
    System.out.println("  class = " + clazz);
    System.out.println("  fieldName = " + fieldName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static void staticFieldAccessLookup(final boolean read,
      final Class clazz, final String fieldName,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("staticFieldAccessLookup");
    System.out.println("  read = " + read);
    System.out.println("  class = " + clazz);
    System.out.println("  fieldName = " + fieldName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void beforeIntrinsicLockAcquisition(final Object lockObject,
      final boolean lockIsThis, final boolean lockIsClass,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("beforeIntrinsicLockAcquisition");
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  lockIsThis = " + lockIsThis);
    System.out.println("  lockIsClass = " + lockIsClass);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void afterIntrinsicLockAcquisition(final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("afterIntrinsicLockAcquisition");
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
  
  public static synchronized void afterIntrinsicLockRelease(final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("afterIntrinsicLockRelease");
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void intrinsicLockWait(final boolean before,
      final Object lockObject, final ClassPhantomReference withinClass, final int line) {
    System.out.println("intrinsicLockWait");
    System.out.println("  before = " + before);
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
  
  public static synchronized void methodCall(final boolean before, final Object receiver,
      final String enclosingFileName, final String enclosingLocationName,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("methodCall");
    System.out.println("  before = " + before);
    System.out.println("  receiver = " + objectToString(receiver));
    System.out.println("  enclosingFileName = " + enclosingFileName);
    System.out.println("  enclosingLocationName = " + enclosingLocationName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void constructorCall(final boolean before,
      final String enclosingFileName,
      final String enclosingLocationName, final ClassPhantomReference withinClass,
      final int line) {
    System.out.println("constructorCall");
    System.out.println("  before = " + before);
    System.out.println("  enclosingFileName = " + enclosingFileName);
    System.out.println("  enclosingLocationName = " + enclosingLocationName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
  
  public static synchronized void constructorExecution(final boolean before,
      final Object receiver, final ClassPhantomReference withinClass, final int line) {
    System.out.println("constructorExecution");
    System.out.println("  before = " + before);
    System.out.println("  receiver = " + objectToString(receiver));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void beforeUtilConcurrentLockAcquisitionAttempt(
      final Object lockObject, final ClassPhantomReference withinClass, final int line) {
    System.out.println("beforeUtilConcurrentLockAcquisitionAttempt");
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void afterUtilConcurrentLockAcquisitionAttempt(
      final boolean gotTheLock, final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("afterUtilConcurrentLockAcquisitionAttempt");
    System.out.println("  gotTheLock = " + gotTheLock);
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }

  public static synchronized void afterUtilConcurrentLockReleaseAttempt(
      final boolean releasedTheLock, final Object lockObject,
      final ClassPhantomReference withinClass, final int line) {
    System.out.println("afterUtilConcurrentLockReleaseAttempt");
    System.out.println("  releasedTheLock = " + releasedTheLock);
    System.out.println("  lockObject = " + objectToString(lockObject));
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
    System.out.flush();
  }
}
