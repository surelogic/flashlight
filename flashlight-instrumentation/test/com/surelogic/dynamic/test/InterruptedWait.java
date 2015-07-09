package com.surelogic.dynamic.test;

public class InterruptedWait {
    public static void main(String[] args) {	    	
    	reenterAndWait();
    }
	
	private static synchronized void reenterAndWait() {
		final Object lock = TestLockOrdering.class;
		synchronized (lock) {
			final Thread main = Thread.currentThread();
			Thread t = new Thread() {
				@Override public void run() {
					try {
						System.out.println("Sleeping");
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					main.interrupt();			
					System.out.println("Done interrupting main thread");
					/*
					// Wake up the first thread				
					synchronized (lock) {								

						lock.notifyAll();
						System.out.println("Done notifying threads");
					}
					*/
				}
			};			
			t.start();	
			System.out.println("Thread started");
			try {				
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Done waiting");
		}		
	}
}
