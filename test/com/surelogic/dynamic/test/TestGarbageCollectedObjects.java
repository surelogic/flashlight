package com.surelogic.dynamic.test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestGarbageCollectedObjects {
    public static void main(String[] args) {
    	final Queue q = new LinkedBlockingQueue();
    	new Thread() {
    		@Override public void run() {
    			for(int i=0; i<1000; i++) {    				
    				q.offer(Integer.toString(i));
    			}
    		}
    	}.start();
    	new Thread() {
    		@Override public void run() {
    			for(int i=0; i<1000; i++) {
    				System.out.println(q.poll());
    				System.gc();
    			}
    		}
    	}.start();
    }
}
