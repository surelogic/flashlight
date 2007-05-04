package com.surelogic.dynamic.test;

public class ReadWriteFinalStatic {

  static final Object o1 = new Object();

  static Object o2;

  static {
    o2 = o1;
  }

  public static void main(String[] args) {
    o2 = o1;
    Object obj = o2;
    obj = o1;
    o2 = obj;
  }
}
