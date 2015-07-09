package com.surelogic.dynamic.test;

public class ReadWriteConstructorStatic {

  static Object o1 = new Object();

  static Object o2;

  static int i1 = 5;

  static int i2;

  static {
    o2 = new Object();
    i2 = 5;
  }

  public static void main(String[] args) {
    o1 = "Hi";
    i1 = i2 + 6;
  }
}
