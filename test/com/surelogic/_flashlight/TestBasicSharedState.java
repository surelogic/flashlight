package com.surelogic._flashlight;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

public class TestBasicSharedState extends TestCase {

	private final static int EVENTS = 5000;
	private final static int THREADS = 30;

	private final CountDownLatch startGate = new CountDownLatch(1);
	private final CountDownLatch endGate = new CountDownLatch(THREADS);

	static class A {
		int i;
		int j;
		static String s;
	}

	private Field f_i, f_s;

	private final A o1 = new A();

	private class ProgThread extends Thread {
		@Override
		public void run() {
			try {
				startGate.await();
			} catch (InterruptedException e) {
				// ignore, just go
			}
			for (int i = 0; i < EVENTS; i++) {
				Store.fieldRead(o1, f_i, null);
				Store.fieldWrite(o1, f_i, null);
				Store.fieldRead(null, f_s, null);
				Store.fieldWrite(null, f_s, null);
			}
			endGate.countDown();
		}
	}

	static class OutputStrategyCounter extends EventVisitor {

		public long fieldDefinitionCt = 0;

		@Override
		void visit(FieldDefinition e) {
			fieldDefinitionCt++;
		}

		public long fieldReadInstanceCt = 0;

		@Override
		void visit(FieldReadInstance e) {
			fieldReadInstanceCt++;
		}

		public long fieldReadStaticCt = 0;

		@Override
		void visit(FieldReadStatic e) {
			fieldReadStaticCt++;
		}

		public long fieldWriteInstanceCt = 0;

		@Override
		void visit(FieldWriteInstance e) {
			fieldWriteInstanceCt++;
		}

		public long fieldWriteStaticCt = 0;

		@Override
		void visit(FieldWriteStatic e) {
			fieldWriteStaticCt++;
		}

		public long finalEventCt = 0;

		@Override
		void visit(FinalEvent e) {
			finalEventCt++;
		}

		public long objectDefinitionCt = 0;

		@Override
		void visit(ObjectDefinition e) {
			objectDefinitionCt++;
		}

		public long singleThreadedFieldInstanceCt = 0;

		@Override
		void visit(SingleThreadedFieldInstance e) {
			singleThreadedFieldInstanceCt++;
		}

		public long singleThreadedFieldStaticCt = 0;

		@Override
		void visit(SingleThreadedFieldStatic e) {
			singleThreadedFieldStaticCt++;
		}

		public long timeCt = 0;

		@Override
		void visit(Time e) {
			timeCt++;
		}

	}

	private final OutputStrategyCounter f_osc = new OutputStrategyCounter();

	public void test() {
		System.setProperty("FL_NO_SPY", "true");
		try {
			f_i = A.class.getDeclaredField("i");
			f_s = A.class.getDeclaredField("s");
		} catch (NoSuchFieldException e1) {
			fail();
		}
		Store.setOutputStrategy(f_osc);
		for (int i = 0; i < THREADS; i++) {
			Thread t = new ProgThread();
			t.start();
		}
		startGate.countDown();
		try {
			endGate.await();
			Store.shutdown();
			/*
			 * OK, now check the counters.
			 */
			assertEquals(1, f_osc.finalEventCt);
			assertEquals(1, f_osc.timeCt);
			assertEquals(2, f_osc.fieldDefinitionCt);
			assertEquals(THREADS + 3, f_osc.objectDefinitionCt);
			assertEquals(EVENTS * THREADS, f_osc.fieldReadInstanceCt);
			assertEquals(EVENTS * THREADS, f_osc.fieldWriteInstanceCt);
			assertEquals(EVENTS * THREADS, f_osc.fieldReadStaticCt);
			assertEquals(EVENTS * THREADS, f_osc.fieldWriteStaticCt);
			assertEquals(0, f_osc.singleThreadedFieldInstanceCt);
			assertEquals(0, f_osc.singleThreadedFieldStaticCt);
		} catch (InterruptedException e) {
			fail("interrupted during end gate await");
		}
	}
}
