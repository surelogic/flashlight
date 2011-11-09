package com.surelogic.dynamic.test;

public class IntrinsicLockingA {

	void dynamicClass() {
		synchronized (this.getClass()) {
			System.out.println("dynamicClass()");
		}
	}

	void staticClass() {
		synchronized (IntrinsicLockingA.class) {
			System.out.println("staticClass()");
		}
	}
}
