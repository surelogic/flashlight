package com.surelogic.dynamic.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UtilConcurrentReentrantLock2 implements Runnable {

	final Lock f_lock = new ReentrantLock();
	private final CountDownLatch f_taskStartGate = new CountDownLatch(1);

	public void run() {
		try {
			f_taskStartGate.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			f_lock.lockInterruptibly();
			try {
				// interrupt possible
			} finally {
				f_lock.unlock();
			}
		} catch (InterruptedException e) {
			System.out.println("lockInterruptibly() was interrupted.");
		}
	}

	void doWork() throws InterruptedException {
		final Thread t = new Thread(this);
		t.start();
		f_lock.lock();
		try {
			f_taskStartGate.countDown();
			Thread.sleep(500);
			t.interrupt();
		} finally {
			f_lock.unlock();
		}
		System.out.print("done...");
	}

	public static void main(String[] args) throws InterruptedException {
		UtilConcurrentReentrantLock2 ucl = new UtilConcurrentReentrantLock2();
		ucl.doWork();
	}
}
