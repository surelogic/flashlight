package com.surelogic.dynamic.test;

public class IntrinsicLocking {

	static final Object lock = new Object();

	static synchronized void staticSync() {
		System.out.println("staticSync()");
	}

	static void staticInSync() {
		synchronized (IntrinsicLocking.class) {
			System.out.println("staticInSync()");
		}
	}

	static int staticGetLock() {
		synchronized (lock) {
			return 5;
		}
	}

	synchronized int syncGet() {
		return 5;
	}

	synchronized void lock() {
		synchronized (lock) {
			System.out.println("lock()");
		}
	}

	public static void main(String[] args) {
		IntrinsicLocking o = new IntrinsicLocking();
		staticSync();
		staticInSync();
		int i1 = staticGetLock();
		i1 = i1 + o.syncGet();
		o.lock();

		// does dynamic class locking do the right thing?
		IntrinsicLockingA a = new IntrinsicLockingA();
		IntrinsicLockingB b = new IntrinsicLockingB();
		IntrinsicLockingA ab = new IntrinsicLockingB();

		a.dynamicClass(); // on A
		b.dynamicClass(); // on B
		ab.dynamicClass(); // on B
		
		a.staticClass(); // on A
		b.staticClass(); // on A
		ab.staticClass(); // on A
	}
}
