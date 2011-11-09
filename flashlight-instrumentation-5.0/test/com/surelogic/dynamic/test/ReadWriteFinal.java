package com.surelogic.dynamic.test;

public class ReadWriteFinal {

  final Object o1 = new Object();

  Object o2;

  public ReadWriteFinal() {
    o2 = o1;
  }

  public static void main(String[] args) {
    ReadWriteFinal o = new ReadWriteFinal();
    o.o2 = o.o1;
    Object obj = o.o2;
    obj = o.o1;
    o.o2 = obj;
  }
}
