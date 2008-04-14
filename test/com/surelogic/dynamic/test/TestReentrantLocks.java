package com.surelogic.dynamic.test;

public class TestReentrantLocks {
	static Object a = new Object();
	static Object b = a;

	public static void main(String[] args) {	
		ab();
		ba();
	}

	static void ab() {
		synchronized (a) {
			synchronized (b) {
				System.out.println("ab");
			}
		}
	}

	static void ba() {
		synchronized (b) {
			synchronized (a) {
				System.out.println("ba");
			}
		}
	}
}

