package com.surelogic.dynamic.test;

public class TestLockSet {

	int a;

	public static void main(String[] args) {
		new TestLockSet().go();
	}

	void go() {
		final Thread one = new Thread(new Z());
		final Thread two = new Thread(new Y());
		one.start();
		two.start();
		try {
			one.join();
			two.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class Z implements Runnable {
		public void run() {
			for (int i = 0; i < 2; i++) {
				a--;
			}
		}
	}

	private class Y implements Runnable {
		public void run() {
			for (int i = 1; i < 2; i++) {
				a++;
			}
		}
	}
}
