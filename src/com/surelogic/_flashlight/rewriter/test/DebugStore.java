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
}
