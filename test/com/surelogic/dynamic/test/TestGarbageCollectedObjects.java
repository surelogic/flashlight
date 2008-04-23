package com.surelogic.dynamic.test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestGarbageCollectedObjects {
    static final int MB = 1024*1024;
	
    static class Holder {
        int i;
        
        Holder(int i) {
        	this.i = i;
        }
    }
    
	public static void main(String[] args) {
    	final Queue q = new LinkedBlockingQueue();
    	new Thread() {
    		@Override public void run() {
    			for(int i=0; i<1000; i++) {    	
    				try {
    					q.offer(new Holder(i));
    					byte[] buf = new byte[MB];
    					buf[0] = (byte) i;
    					q.offer(buf);
    				} catch (OutOfMemoryError e) {
    					System.gc();
    				}
    			}
    		}
    	}.start();
    	new Thread() {
    		@Override public void run() {
    			for(int i=0; i<2000; i++) {
    				Object o = q.poll();
    				if (o != null) {
    					if (o instanceof byte[]) {
    						byte[] buf = (byte[]) o;
                            System.out.println(buf+"[0] = "+buf[0]);    						
    					} else {
    						Holder h = (Holder) o;
     					    System.out.println(h.i);    					
    					}
    					System.gc();
    				}
    			}
    		}
    	}.start();
    }
}
