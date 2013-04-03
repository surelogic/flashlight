package com.surelogic.dynamic.test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This test will flag no fields, although one of them is actually incorrect.
 * 
 * @author nathan
 * 
 */
public class TestLockSetReadWriteLock {
    ReentrantReadWriteLock rwla = new ReentrantReadWriteLock();
    ReentrantReadWriteLock rwlb = new ReentrantReadWriteLock();

    int a;
    int b;

    public static void main(String[] args) {
        new TestLockSetReadWriteLock().go();
    }

    private void go() {
        Thread one = new Thread(new Z());
        Thread two = new Thread(new Y());
        one.start();
        two.start();
        try {
            one.join();
            two.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class Z implements Runnable {
        public synchronized void run() {
            rwla.writeLock().lock();
            for (int i = 0; i < 2; i++) {
                a--;
            }
            rwla.writeLock().unlock();
            rwlb.writeLock().lock();
            for (int i = 0; i < 2; i++) {
                b--;
            }
            rwlb.writeLock().unlock();
        }
    }

    class Y implements Runnable {
        public synchronized void run() {
            rwla.readLock().lock();
            rwlb.readLock().lock();
            for (int i = 0; i < 2; i++) {
                a = b + 1;
            }
            for (int i = 0; i < 2; i++) {
                a++;
            }
            rwla.readLock().unlock();
            rwlb.readLock().unlock();
        }
    }
}
