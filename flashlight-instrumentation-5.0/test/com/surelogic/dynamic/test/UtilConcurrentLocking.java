package com.surelogic.dynamic.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class UtilConcurrentLocking {

	final Lock lock1 = new ReentrantLock();
	final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	void lock1() {
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

	void lockrw() {
		rwLock.readLock().lock();
		try {
			// do stuff
		} finally {
			rwLock.readLock().unlock();
		}
		Lock r = rwLock.readLock();
		r.lock();
		try {
			// do stuff
		} finally {
			r.unlock();
		}

		rwLock.writeLock().lock();
		try {
			// do stuff
		} finally {
			rwLock.writeLock().unlock();
		}
		Lock w = rwLock.writeLock();
		w.lock();
		try {
			// do stuff
		} finally {
			w.unlock();
		}
	}

	public static void main(String[] args) {
		UtilConcurrentLocking ucl = new UtilConcurrentLocking();
		ucl.lock1();
		ucl.lockrw();
	}
}
