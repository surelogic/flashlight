package com.surelogic._flashlight.rewriter.test;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.surelogic._flashlight.ClassPhantomReference;
import com.surelogic._flashlight.ObjectPhantomReference;
import com.surelogic._flashlight.Phantom;
import com.surelogic._flashlight.StoreDelegate;

public class DebugStore {
  /**
   * We cannot use {@link System#out} because it may have been redirected to 
   * use a locally implemented stream class.  This could cause recursive stack
   * overflow problems if the new stream class is also instrumented.  So we
   * capture our own version of the standard output stream.
   */
  private static final PrintStream stdOut;
  
  
  
  static {
    final FileOutputStream fdOut = new FileOutputStream(FileDescriptor.out);
    stdOut = new PrintStream(new BufferedOutputStream(fdOut, 128), true);
  }
  
  
  
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

  /**
   * Use this instead of o.toString() because if toString() is overridden we can
   * end up having endless recursive calls to the the methodCall() event.
   */
  private static String objectToString(final Object o) {
    // From Object.toString();
    if (o == null) {
      return "null";
    } else {
      return o.getClass().getName() + "@" +
        Integer.toHexString(System.identityHashCode(o));
    }
  }
  
  public static synchronized void instanceFieldAccess(
      final boolean read, final Object receiver, final int fieldID,
      final long siteId, final ClassPhantomReference dcPhantom, 
      final Class<?> declaringClass) {
    stdOut.println("instanceFieldAccess");
    stdOut.println("  read = " + read);
    stdOut.println("  receiver = " + objectToString(receiver));
    stdOut.println("  fieldID = " + fieldID);
    stdOut.println("  siteID = " + siteId);
    stdOut.println("  dcPhantom = " + objectToString(dcPhantom));
    stdOut.println("  declaringClass = " + objectToString(declaringClass));
    stdOut.flush();
  }

  public static synchronized void staticFieldAccess(
      final boolean read, final int fieldID, final long siteId,
      final ClassPhantomReference dcPhantom, final Class<?> declaringClass) {
    stdOut.println("staticFieldAccess");
    stdOut.println("  read = " + read);
    stdOut.println("  fieldID = " + fieldID);
    stdOut.println("  siteID = " + siteId);
    stdOut.println("  dcPhantom = " + objectToString(dcPhantom));
    stdOut.println("  declaringClass = " + objectToString(declaringClass));
    stdOut.flush();
  }

//  public static synchronized void instanceFieldAccessLookup(
//      final boolean read, final Object receiver,
//      final Class clazz, final String fieldName,
//      final long siteId) {
//    stdOut.println("instanceFieldAccessLookup");
//    stdOut.println("  read = " + read);
//    stdOut.println("  receiver = " + objectToString(receiver));
//    stdOut.println("  class = " + clazz);
//    stdOut.println("  fieldName = " + fieldName);
//    stdOut.println("  siteID = " + siteId);
//    stdOut.flush();
//  }

//  public static synchronized void staticFieldAccessLookup(final boolean read,
//      final Class clazz, final String fieldName,
//      final long siteId) {
//    stdOut.println("staticFieldAccessLookup");
//    stdOut.println("  read = " + read);
//    stdOut.println("  class = " + clazz);
//    stdOut.println("  fieldName = " + fieldName);
//    stdOut.println("  siteID = " + siteId);
//    stdOut.flush();
//  }

  public static synchronized void indirectAccess(
      final Object receiver, final long siteId) {
    stdOut.println("indirectAccess");
    stdOut.println("  receiver = " + objectToString(receiver));
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }
  
  public static synchronized void arrayAccess(
      final boolean read, final Object receiver, final int index, final long siteId) {
    stdOut.println("arrayAccess");
    stdOut.println("  read = " + read);
    stdOut.println("  receiver = " + receiver);
    stdOut.println("  index = " + index);
    stdOut.println("  siteId = " + siteId);
    stdOut.flush();
  }
  
  public static synchronized void beforeIntrinsicLockAcquisition(
      final Object lockObject, final boolean lockIsThis,
      final boolean lockIsClass, final long siteId) {
    stdOut.println("beforeIntrinsicLockAcquisition");
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  lockIsThis = " + lockIsThis);
    stdOut.println("  lockIsClass = " + lockIsClass);
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void afterIntrinsicLockAcquisition(
      final Object lockObject, final boolean lockIsThis, final long siteId) {
    stdOut.println("afterIntrinsicLockAcquisition");
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  lockIsThis = " + lockIsThis);
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }
  
  public static synchronized void afterIntrinsicLockRelease(
      final Object lockObject, final boolean lockIsThis, final long siteId) {
    stdOut.println("afterIntrinsicLockRelease");
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  lockIsThis = " + lockIsThis);
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void intrinsicLockWait(
      final boolean before, final Object lockObject, final boolean lockIsThis, final long siteId) {
    stdOut.println("intrinsicLockWait");
    stdOut.println("  before = " + before);
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  lockIsThis = " + lockIsThis);
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void methodExecution(
      final boolean before, final long siteId) {
    stdOut.println("methodExecution");
    stdOut.println("  before = " + before);
    stdOut.flush();
  }
  
  public static synchronized void methodCall(
      final boolean before, final Object receiver, final long siteId) {
    stdOut.println("methodCall");
    stdOut.println("  before = " + before);
    stdOut.println("  receiver = " + objectToString(receiver));
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public synchronized static void classInit(final boolean before, final Class<?> clazz) {
    stdOut.println("classInit");
    stdOut.println("  before = " + before);
    stdOut.println("  clazz = " + objectToString(clazz));
    stdOut.flush();
  }
  
  public static synchronized void constructorCall(
      final boolean before, final long siteId) {
    stdOut.println("constructorCall");
    stdOut.println("  before = " + before);
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }
  
  public static synchronized void constructorExecution(
      final boolean before, final Object receiver, final long siteId) {
    stdOut.println("constructorExecution");
    stdOut.println("  before = " + before);
    stdOut.println("  receiver = " + objectToString(receiver));
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void beforeUtilConcurrentLockAcquisitionAttempt(
      final Object lockObject, final long siteId) {
    stdOut.println("beforeUtilConcurrentLockAcquisitionAttempt");
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void afterUtilConcurrentLockAcquisitionAttempt(
      final boolean gotTheLock, final Object lockObject, final long siteId) {
    stdOut.println("afterUtilConcurrentLockAcquisitionAttempt");
    stdOut.println("  gotTheLock = " + gotTheLock);
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void afterUtilConcurrentLockReleaseAttempt(
      final boolean releasedTheLock, final Object lockObject, final long siteId) {
    stdOut.println("afterUtilConcurrentLockReleaseAttempt");
    stdOut.println("  releasedTheLock = " + releasedTheLock);
    stdOut.println("  lockObject = " + objectToString(lockObject));
    stdOut.println("  siteID = " + siteId);
    stdOut.flush();
  }

  public static synchronized void instanceFieldInit(
      final Object receiver, final int fieldId, final Object value) {
    stdOut.println("instanceFieldInit");
    stdOut.println("  receiver = " + objectToString(receiver));
    stdOut.println("  fieldId = " + fieldId);
    stdOut.println("  value = " + objectToString(value));
  }
  
  public static synchronized void staticFieldInit(final int fieldId, final Object value) {
    stdOut.println("staticFieldInit");
    stdOut.println("  fieldId = " + fieldId);
    stdOut.println("  value = " + objectToString(value));
  }

  public static synchronized int getFieldId(final String clazz, final String field) {
    stdOut.println("getFieldId");
    stdOut.println("  class = " + clazz);
    stdOut.println("  value = " + field);
    return -1;
  }

  public static synchronized void happensBeforeThread(
      final long nanoTime, final Thread callee, final String id, 
      final long siteId, final String typeName) {
    stdOut.println("happensBeforeThread");
    stdOut.println("  nanoTime = " + nanoTime);
    stdOut.println("  callee = " + objectToString(callee));
    stdOut.println("  id = " + id);
    stdOut.println("  siteID = " + siteId);
    stdOut.println("  typeName = " + typeName);
  }
  
  public static synchronized void happensBeforeObject(
      final long nanoTime, final Object object, final String id, 
      final long siteId, final String typeName) {
    stdOut.println("happensBeforeObject");
    stdOut.println("  nanoTime = " + nanoTime);
    stdOut.println("  object = " + objectToString(object));
    stdOut.println("  id = " + id);
    stdOut.println("  siteID = " + siteId);
    stdOut.println("  typeName = " + typeName);
  }
  
  public static synchronized void happensBeforeCollection(
      final long nanoTime, final Object item, final Object collection,
      final String id, final long siteId, final String typeName) {
    stdOut.println("happensBeforeCollection");
    stdOut.println("  nanoTime = " + nanoTime);
    stdOut.println("  item = " + objectToString(item));
    stdOut.println("  collection = " + objectToString(collection));
    stdOut.println("  id = " + id);
    stdOut.println("  siteID = " + siteId);
    stdOut.println("  typeName = " + typeName);
  }
}
