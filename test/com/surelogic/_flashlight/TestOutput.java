package com.surelogic._flashlight;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

public class TestOutput extends TestCase {

	private final static int EVENTS = 5000;
	private final static int THREADS = 30;

	private final CountDownLatch startGate = new CountDownLatch(1);
	private final CountDownLatch midGate = new CountDownLatch(THREADS);
	private final CountDownLatch endGate = new CountDownLatch(THREADS);

	static class A {
		int i;
		int j;
		static String s;
	}

	private Field f_i, f_j, f_s;

	private final A o1 = new A();

	private class ProgThread extends Thread {
		@Override
		public void run() {
			try {
				startGate.await();
			} catch (InterruptedException e) {
				// ignore, just go
			}
			/*
			 * Output a few reads from a thread local object before the shared
			 * one gets lots of events. Hopefully, these will get removed from
			 * the event cache within the Refinery. If not you'll see a failure
			 * with up to (THREADS * 2) extra events.
			 */
			A o = new A();
//			Store.fieldAccess(true, o, f_j, null, 1);
//			Store.fieldAccess(true, o, f_j, null, 1);
			o = null;
			midGate.countDown();
			try {
				midGate.await();
			} catch (InterruptedException e) {
				// ignore, just go
			}
			for (int i = 0; i < EVENTS; i++) {
//				Store.fieldAccess(true, o1, f_i, null, 1);
//				Store.fieldAccess(false, o1, f_i, null, 1);
//				Store.fieldAccess(true, null, f_s, null, 1);
//				Store.fieldAccess(false, null, f_s, null, 1);
			}
			endGate.countDown();
		}
	}

	public void test() {
		System.setProperty("FL_NO_SPY", "true");
		try {
			f_i = A.class.getDeclaredField("i");
			f_j = A.class.getDeclaredField("j");
			f_s = A.class.getDeclaredField("s");
		} catch (NoSuchFieldException e1) {
			fail();
		}
		for (int i = 0; i < THREADS; i++) {
			Thread t = new ProgThread();
			t.start();
		}
		startGate.countDown();
		try {
			endGate.await();
			Store.shutdown();
		} catch (InterruptedException e) {
			fail("interrupted during end gate await");
		}
	}
}
