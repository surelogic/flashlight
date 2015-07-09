package com.surelogic.dynamic.test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UtilConcurrentReentrantLock0 {

	final Lock f_lock = new ReentrantLock();

	void doWork() {
		try {
			f_lock.unlock();
		} catch (Exception e) {
			System.out.println("unlock() threw exception as expected.");
		}
		System.out.print("done...");
	}

	public static void main(String[] args) throws InterruptedException {
		UtilConcurrentReentrantLock0 ucl = new UtilConcurrentReentrantLock0();
		ucl.doWork();
	}
}
