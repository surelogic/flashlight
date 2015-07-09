package com.surelogic.dynamic.test;

public class TestLockOrdering {
    static Object a = new Object();
    static Object b = new Object();
	
    public static void main(String[] args) {	
    	ab();
    	ba();
    }
    
    static void ab() {
    	synchronized (a) {
    		synchronized (b) {
    			System.out.println("ab");
    		}
    	}
    }
    
    static void ba() {
    	synchronized (b) {
    		synchronized (a) {
    			System.out.println("ba");
    		}
    	}
    }
}
