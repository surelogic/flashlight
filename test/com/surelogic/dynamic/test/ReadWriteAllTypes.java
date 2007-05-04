package com.surelogic.dynamic.test;

public class ReadWriteAllTypes {

	Object o;

	String str; // a subtype of Object

	short s;

	int i;

	long l;

	double d;

	float f;

	byte b;

	char c;

	boolean bool;

	public static void main(String[] args) {
		ReadWriteAllTypes o = new ReadWriteAllTypes();
		o.o = new Object();
		o.o = o.o;
		o.str = "Hi";
		o.str = o.str + " there";
		o.s = 1;
		o.s = o.s;
		o.i = 1;
		o.i = o.i;
		o.l = 1;
		o.l = o.l;
		o.d = 1;
		o.d = o.d;
		o.f = 1;
		o.f = o.f;
		o.b = 1;
		o.b = o.b;
		o.c = 'a';
		char cLocal = o.c;
		o.c = cLocal;
		o.bool = true;
		o.bool = !o.bool;
	}
}
