package com.surelogic.dynamic.test;

public class ReadWriteAllTypesStatic {

	static Object o;

	static String str; // a subtype of Object

	static short s;

	static int i;

	static long l;

	static double d;

	static float f;

	static byte b;

	static char c;

	static boolean bool;

	public static void main(String[] args) {
		Object obj;
		o = new Object();
		obj = o;
		o = obj;
		str = "Hi";
		str = str + " there";
		s = 1;
		obj = s;
		i = 1;
		obj = i;
		l = 1;
		obj = l;
		d = 1;
		obj = d;
		f = 1;
		obj = f;
		b = 1;
		obj = b;
		c = 'a';
		char cLocal = c;
		c = cLocal;
		bool = true;
		bool = !bool;
	}
}
