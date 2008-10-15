package com.surelogic._flashlight.common;


public class LongSet extends LongMap<Boolean> {
	// FIX can this be a more space-efficient
	// by not having any entries?
	public void add(long e) {
		this.put(e, Boolean.TRUE);
	}

	public boolean contains(long e) {
		return this.get(e) != null;
	}
}
