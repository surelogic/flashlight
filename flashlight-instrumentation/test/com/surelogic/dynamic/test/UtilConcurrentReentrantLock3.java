package com.surelogic.dynamic.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UtilConcurrentReentrantLock3 implements Runnable {

	final Lock f_lock = new ReentrantLock();
	private final CountDownLatch f_taskStartGate = new CountDownLatch(1);
	private final CountDownLatch f_task0 = new CountDownLatch(1);

	public void run() {
		try {
			f_taskStartGate.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (f_lock.tryLock()) {
			try {
				System.out.println("GOOD : Got the lock as expected");
			} finally {
				f_lock.unlock();
			}
		} else {
			System.out.println("BAD : Didn't get the lock");
		}

		try {
			f_task0.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (f_lock.tryLock()) {
			try {
				System.out.println("BAD : Got the lock");
			} finally {
				f_lock.unlock();
			}
		} else {
			System.out.println("GOOD : Didn't get the lock  as expected");
		}
	}

	void doWork() throws InterruptedException {
		final Thread t = new Thread(this);
		t.start();
		f_taskStartGate.countDown();
		Thread.sleep(1000);
		f_lock.lock();
		try {
			f_task0.countDown();
			Thread.sleep(500);
		} finally {
			f_lock.unlock();
		}
		System.out.print("done...");
	}

	public static void main(String[] args) throws InterruptedException {
		UtilConcurrentReentrantLock3 ucl = new UtilConcurrentReentrantLock3();
		ucl.doWork();
	}
}
