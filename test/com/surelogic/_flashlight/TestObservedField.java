package com.surelogic._flashlight;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

public class TestObservedField extends TestCase {

	/**
	 * Ensure this queue has enough room to fit all the new fields or the test
	 * case will block because
	 * {@link ObservedField#getInstance(Field, BlockingQueue)} uses
	 * {@link BlockingQueue#put(Object)} which can block.
	 */
	private static final BlockingQueue<List<Event>> Q = 
		new ArrayBlockingQueue<List<Event>>(4);

	private static final Store.State state = new Store.State(Q, new ArrayList<Event>());
	
	private final static int THREADS = 30;

	static volatile int JUNK = 10;

	private final CountDownLatch startGate = new CountDownLatch(1);

	private volatile CountDownLatch endGate = new CountDownLatch(THREADS);

	private volatile boolean failureInProgThreadChecks = false;

	private final List<ObservedField> l = Collections
			.synchronizedList(new LinkedList<ObservedField>());

	private class ProgThread extends Thread {
		@Override
		public void run() {
			try {
				startGate.await();
			} catch (InterruptedException e) {
				// ignore, just go
			}
			try {
				ObservedField of, last = null;
				for (int i = 0; i < 10; i++) {
					of = ObservedField.getInstance(f_threads, state);
					l.add(of);
					if (last != null)
						if (last != of)
							failureInProgThreadChecks = true;
					last = of;
				}
			} finally {
				endGate.countDown();
			}
		}
	}

	private Field f_threads, f_junk, f_startGate, f_endGate;

	public void test() {
		try {
			f_threads = this.getClass().getDeclaredField("THREADS");
			f_junk = this.getClass().getDeclaredField("JUNK");
			f_startGate = this.getClass().getDeclaredField("startGate");
			f_endGate = this.getClass().getDeclaredField("endGate");
		} catch (NoSuchFieldException e1) {
			fail();
		}
		/*
		 * This is not a great test of the class invariant (i.e., object
		 * identity is the same a semantic identity), however it might uncover a
		 * problem.
		 */
		for (int i = 0; i < THREADS; i++) {
			Thread t = new ProgThread();
			t.start();
		}
		startGate.countDown();
		try {
			endGate.await();

			assertFalse("ObservedField.getInstance() failed in threads",
					failureInProgThreadChecks);

			ObservedField of = ObservedField.getInstance(f_threads, state);
			assertTrue(of.isFinal());
			assertTrue(of.isStatic());
			assertFalse(of.isVolatile());
			assertEquals("com.surelogic._flashlight.TestObservedField", of
					.getDeclaringType().getName());

			for (ObservedField f : l)
				assertSame(of, f);

			of = ObservedField.getInstance(f_junk, state);
			assertFalse(of.isFinal());
			assertTrue(of.isStatic());
			assertTrue(of.isVolatile());
			assertEquals("com.surelogic._flashlight.TestObservedField", of
					.getDeclaringType().getName());

			of = ObservedField.getInstance(f_startGate, state);
			assertTrue(of.isFinal());
			assertFalse(of.isStatic());
			assertFalse(of.isVolatile());
			assertEquals("com.surelogic._flashlight.TestObservedField", of
					.getDeclaringType().getName());

			of = ObservedField.getInstance(f_endGate, state);
			assertFalse(of.isFinal());
			assertFalse(of.isStatic());
			assertTrue(of.isVolatile());
			assertEquals("com.surelogic._flashlight.TestObservedField", of
					.getDeclaringType().getName());

		} catch (InterruptedException e) {
			fail("interrupted during end gate await");
		}
	}
}
