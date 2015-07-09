package com.surelogic.dynamic.test;

import java.util.Random;

public final class TraceConstructorCalls {

	static class A {
		private int f_i, f_j, f_k;

		{
			f_k = (new Random()).nextInt();
		}

		public int getI() {
			return f_i;
		}

		public int getJ() {
			return f_j;
		}

		public int getK() {
			return f_k;
		}

		public A(int i, int j) {
			this(i);
			f_j = j;
		}

		public A(int i) {
			f_i = i;
		}

		void m2() {
			System.out.println("m2() i=" + getI() + " j=" + getJ());
			m3();
		}

		void m3() {
			System.out.println("m3()");
			m1();
		}
	}

	static class B extends A {

		public B(int i, int j) {
			super(i + 1, j + 1);
		}

	}

	static void m1() {
		System.out.println("static m1()");
	}

	public static void main(String[] args) {
		final B tmc = new B(5, 2);
		m1();
		tmc.m2();

		Runnable r = new Runnable() {
			public void run() {
				tmc.getI();
				tmc.getJ();
				tmc.getK();
			}
		};
		(new Thread(r)).start();
	}

}
