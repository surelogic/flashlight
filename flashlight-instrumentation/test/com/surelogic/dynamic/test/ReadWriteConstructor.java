package com.surelogic.dynamic.test;

public class ReadWriteConstructor {

  Object o1 = new Object();

  Object o2;

  int i1 = 5;

  int i2;

  public ReadWriteConstructor() {
    o2 = new Object();
    i2 = 5;
  }

  public static void main(String[] args) {
    ReadWriteConstructor o = new ReadWriteConstructor();
    o.o1 = "Hi";
    o.i1 = o.i2 + 6;
  }
}
