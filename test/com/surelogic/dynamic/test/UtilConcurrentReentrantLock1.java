package com.surelogic.dynamic.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UtilConcurrentReentrantLock1 implements Runnable {

	final Lock f_lock = new ReentrantLock();
	private final CountDownLatch f_taskStartGate = new CountDownLatch(1);

	public void run() {
		try {
			f_taskStartGate.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		f_lock.lock();
		try {
			System.out.println("lock() OK");
		} finally {
			f_lock.unlock();
		}
	}

	void doWork() throws InterruptedException {
		final Thread t = new Thread(this);
		t.start();
		f_taskStartGate.countDown();
		Thread.sleep(1000);
		System.out.print("done...");
	}

	public static void main(String[] args) throws InterruptedException {
		UtilConcurrentReentrantLock1 ucl = new UtilConcurrentReentrantLock1();
		ucl.doWork();
	}
}
