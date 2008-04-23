package com.surelogic.dynamic.test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestGarbageCollectedObjects {
    static final int MB = 1024*1024;
	
	public static void main(String[] args) {
    	final Queue q = new LinkedBlockingQueue();
    	new Thread() {
    		@Override public void run() {
    			for(int i=0; i<1000; i++) {    	
    				try {
    					q.offer(Integer.toString(i));
    					q.offer(new byte[MB]);
    				} catch (OutOfMemoryError e) {
    					System.gc();
    				}
    			}
    		}
    	}.start();
    	new Thread() {
    		@Override public void run() {
    			for(int i=0; i<1000; i++) {
    				Object o = q.poll();
    				if (o != null) {
    					System.out.println(o);
    					q.poll();
    					System.gc();
    				}
    			}
    		}
    	}.start();
    }
}
