package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class TestPhantom extends TestCase {

	public void test() {
		int drainCount;
		List<IdPhantomReference> goneList = new LinkedList<IdPhantomReference>();

		Object o1 = new Object();
		Thread o2 = new Thread();
		o2.setName("TestThread");
		Class o3 = o2.getClass();
		IdPhantomReference pr1 = Phantom.of(o1);
		ObjectPhantomReference pr1o = Phantom.ofObject(o1);
		assertSame(pr1, pr1o);
		assertEquals(1L, pr1.getId());
		assertTrue(pr1 instanceof ObjectPhantomReference);
		assertFalse(pr1 instanceof ClassPhantomReference);
		assertFalse(pr1 instanceof ThreadPhantomReference);
		//assertEquals("java.lang.Object", pr1o.getType().getName());
		IdPhantomReference pr2 = Phantom.of(o2);
		ThreadPhantomReference pr2t = Phantom.ofThread(o2);
		assertSame(pr2, pr2t);
		//assertEquals("java.lang.Thread", pr2t.getType().getName());
		assertEquals("TestThread", pr2t.getName());
		PhantomReference pr3 = Phantom.of(o3);
		ClassPhantomReference pr3c = Phantom.ofClass(o3);
		assertSame(pr3, pr3c);
		assertEquals("java.lang.Thread", pr3c.getName());
		PhantomReference pr1a = Phantom.of(o1);
		assertSame(pr1, pr1a);
		/*
		 * The below checks are dependent upon the garbage collector.
		 * 
		 * So, sometimes, they might not pass due to the VM.
		 */
		o1 = null;
		gc();
		drainCount = Phantom.drainTo(goneList);
		assertEquals(1, drainCount);
		assertEquals(1, goneList.size());
		assertTrue(goneList.contains(pr1));
		assertFalse(goneList.contains(pr2));
		gc();
		goneList.clear();
		drainCount = Phantom.drainTo(goneList);
		assertEquals(0, drainCount);
		assertEquals(0, goneList.size());
		assertFalse(goneList.contains(pr1));
		assertFalse(goneList.contains(pr2));
		o2 = null;
		gc();
		goneList.clear();
		drainCount = Phantom.drainTo(goneList);
		assertEquals(1, drainCount);
		assertEquals(1, goneList.size());
		assertFalse(goneList.contains(pr1));
		assertTrue(goneList.contains(pr2));
	}

	/**
	 * Runs the garbage collector and sleeps for a bit. This is a bit of a hack
	 * to test the use of {@link PhantomReference}s within our code.
	 */
	public static void gc() {
		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore the interruption
		}

	}
}
