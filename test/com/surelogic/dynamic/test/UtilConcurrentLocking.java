package com.surelogic.dynamic.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class UtilConcurrentLocking {

	final Lock lock1 = new ReentrantLock();
	final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	void lock() {
		lock1.lock();
		try {
			// blocking case
		} finally {
			lock1.unlock();
		}

		try {
			lock1.lockInterruptibly();
			try {
				// interrupt possible
			} finally {
				lock1.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (lock1.tryLock()) {
			try {
				// non-blocking case
			} finally {
				lock1.unlock();
			}
		}

		try {
			if (lock1.tryLock(1, TimeUnit.SECONDS)) {
				try {
					// waits for 1 second, interrupt possible
				} finally {
					lock1.unlock();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
