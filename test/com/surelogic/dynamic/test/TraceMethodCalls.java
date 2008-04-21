package com.surelogic.dynamic.test;

public final class TraceMethodCalls {

	static void m1() {
		System.out.println("static m1()");
	}

	public static void main(String[] args) {
		TraceMethodCalls tmc = new TraceMethodCalls();
		m1();
		tmc.m2();
	}

	private void m2() {
		System.out.println("m2()");
		m3();
	}

	private void m3() {
		System.out.println("m3()");
		m1();
	}
}