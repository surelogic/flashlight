package com.surelogic.dynamic.test;

public class IntrinsicLockingWait {

	static final Object lock = new Object();

	static void sleep(long sec) {
		try {
			Thread.sleep(sec * 1000);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	public static void main(String[] args) {
		class Notifier extends Thread {
			volatile boolean done = false;

			@Override
			public void run() {
				while (true) {
					IntrinsicLockingWait.sleep(1);
					if (done)
						break;
					synchronized (lock) {
						lock.notify();
					}
					synchronized (this) {
						this.notify();
					}
				}
			}
		}
		Notifier n = new Notifier();
		n.start();
		synchronized (lock) {
			try {
				lock.wait();
				System.out.println("lock.wait()");
				lock.wait(10000);
				System.out.println("lock.wait(10000)");
				lock.wait(10000, 10);
				System.out.println("lock.wait(10000, 10)");

			} catch (InterruptedException e) {
			}
		}
		n.done = true;
	}
}
