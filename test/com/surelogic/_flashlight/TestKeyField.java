package com.surelogic._flashlight;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.TestCase;

public class TestKeyField extends TestCase {

	/**
	 * Ensure this queue has enough room to fit all the new fields or the test
	 * case will block because
	 * {@link ObservedField#getInstance(Field, BlockingQueue)} uses
	 * {@link BlockingQueue#put(Object)} which can block.
	 */
	private static final BlockingQueue<Event> Q = new ArrayBlockingQueue<Event>(
			6);

	static class A {
		Object f1;
		Object f2;
		static Object s1;
		static Object s2;
	}

	static class B extends A {
		Object f2;
		static Object s2;
	}

	private Field fa_f1, fa_f2, fa_s1, fa_s2, fb_f1, fb_f2, fb_s1, fb_s2;
	private ObservedField a_f1, a_f2, a_s1, a_s2, b_f1, b_f2, b_s1, b_s2;

	{

	}

	private A o1 = new A();
	private A o2 = new A();
	private A o3 = new A();
	private A o4 = new A();

	@Override
	protected void setUp() throws Exception {

		try {
			fb_f1 = B.class.getDeclaredField("f1");
		} catch (NoSuchFieldException e1) {
			// good
		}
		try {
			fb_s1 = B.class.getDeclaredField("s1");
		} catch (NoSuchFieldException e1) {
			// good
		}
		try {
			fa_f1 = A.class.getDeclaredField("f1");
			fa_f2 = A.class.getDeclaredField("f2");
			fa_s1 = A.class.getDeclaredField("s1");
			fa_s2 = A.class.getDeclaredField("s2");
			fb_f1 = fa_f1;
			fb_f2 = B.class.getDeclaredField("f2");
			fb_s1 = fa_s1;
			fb_s2 = B.class.getDeclaredField("s2");
		} catch (NoSuchFieldException e1) {
			fail();
		}

		a_f1 = ObservedField.getInstance(fa_f1, Q);
		a_f2 = ObservedField.getInstance(fa_f2, Q);
		a_s1 = ObservedField.getInstance(fa_s1, Q);
		a_s2 = ObservedField.getInstance(fa_s2, Q);

		b_f1 = ObservedField.getInstance(fb_f1, Q);
		b_f2 = ObservedField.getInstance(fb_f2, Q);
		b_s1 = ObservedField.getInstance(fb_s1, Q);
		b_s2 = ObservedField.getInstance(fb_s2, Q);
	}

	public void testObservedField() {
		assertSame(a_f1, b_f1);
		assertFalse(a_f2 == b_f2);
		assertSame(a_s1, b_s1);
		assertFalse(a_s2 == b_f2);
	}

	public void testKeyFieldInstance() {
		KeyFieldInstance k10 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o2));
		KeyFieldInstance k11 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o2));
		KeyFieldInstance k20 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o4));
		KeyFieldInstance k21 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o4));
		KeyFieldInstance k22 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o1));
		KeyFieldInstance k23 = new KeyFieldInstance(a_f2, Phantom
				.ofObject(o4));
		KeyFieldInstance k24 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o3));
		assertEquals(k10, k11);
		assertEquals(k20, k21);
		assertFalse(k10.equals(k20));
		assertFalse(k20.equals(k10));
		assertFalse(k20.equals(k22));
		assertFalse(k22.equals(k20));
		assertFalse(k20.equals(k23));
		assertFalse(k23.equals(k20));
		assertFalse(k20.equals(k24));
		assertFalse(k24.equals(k20));
		assertFalse(k20.equals(null));
		Set<KeyField> keys = new HashSet<KeyField>();
		keys.add(k10);
		assertEquals(1, keys.size());
		keys.add(k10);
		assertEquals(1, keys.size());
		keys.add(k11);
		assertEquals(1, keys.size());
		keys.add(k20);
		assertEquals(2, keys.size());
		keys.add(k21);
		assertEquals(2, keys.size());
		keys.add(k22);
		assertEquals(3, keys.size());
	}

	public void testKeyFieldStatic() {
		KeyFieldStatic k10 = new KeyFieldStatic(a_s2);
		KeyFieldStatic k11 = new KeyFieldStatic(a_s2);
		KeyFieldStatic k12 = new KeyFieldStatic(b_s2);
		KeyFieldStatic k13 = new KeyFieldStatic(a_s1);
		KeyFieldStatic k14 = new KeyFieldStatic(a_s1);
		assertEquals(k10, k11);
		assertEquals(k11, k10);
		assertFalse(k10.equals(k12));
		assertFalse(k12.equals(k10));
		assertFalse(k10.equals(k13));
		assertFalse(k13.equals(k10));
		assertFalse(k10.equals(k14));
		assertFalse(k14.equals(k10));
		assertEquals(k13, k14);
		assertEquals(k14, k13);
		Set<KeyField> keys = new HashSet<KeyField>();
		keys.add(k10);
		assertEquals(1, keys.size());
		keys.add(k10);
		assertEquals(1, keys.size());
		keys.add(k11);
		assertEquals(1, keys.size());
		keys.add(k12);
		assertEquals(2, keys.size());
		keys.add(k13);
		assertEquals(3, keys.size());
		keys.add(k14);
		assertEquals(3, keys.size());
	}

	public void testMixedKeys() {
		KeyFieldInstance k10 = new KeyFieldInstance(a_f1, Phantom
				.ofObject(o2));
		KeyFieldStatic k11 = new KeyFieldStatic(a_s1);
		assertFalse(k10.equals(k11));
		assertFalse(k11.equals(k10));
		Set<KeyField> keys = new HashSet<KeyField>();
		keys.add(k10);
		assertEquals(1, keys.size());
		keys.add(k10);
		assertEquals(1, keys.size());
		keys.add(k11);
		assertEquals(2, keys.size());
		keys.add(k11);
		assertEquals(2, keys.size());
	}
}
