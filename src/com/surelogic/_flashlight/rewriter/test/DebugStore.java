package com.surelogic._flashlight.rewriter.test;

import java.lang.reflect.Field;

public class DebugStore {
  public static void fieldAccess(final boolean read, final Object receiver,
      final Field field, Class<?> withinClass, final int line) {
    System.out.println("fieldAccess");
    System.out.println("  read = " + read);
    System.out.println("  receiver = " + receiver);
    System.out.println("  field = " + field);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
  }

  public static void beforeIntrinsicLockAcquisition(final Object lockObject,
      final boolean lockIsThis, final boolean lockIsClass,
      Class<?> withinClass, final int line) {
    System.out.println("beforeIntrinsicLockAcquisition");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  lockIsThis = " + lockIsThis);
    System.out.println("  lockIsClass = " + lockIsClass);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
  }

  public static void afterIntrinsicLockAcquisition(final Object lockObject,
      Class<?> withinClass, final int line) {
    System.out.println("afterIntrinsicLockAcquisition");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
  }
  
  public static void afterIntrinsicLockRelease(final Object lockObject,
      Class<?> withinClass, final int line) {
    System.out.println("afterIntrinsicLockRelease");
    System.out.println("  lockObject = " + lockObject);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
  }

  public static void methodCall(final boolean before, final Object receiver,
      final String enclosingFileName, final String enclosingLocationName,
      Class<?> withinClass, final int line) {
    System.out.println("methodCall");
    System.out.println("  before = " + before);
    System.out.println("  receiver = " + receiver);
    System.out.println("  enclosingFileName = " + enclosingFileName);
    System.out.println("  enclosingLocationName = " + enclosingLocationName);
    System.out.println("  withinClass = " + withinClass);
    System.out.println("  line = " + line);
  }
}
