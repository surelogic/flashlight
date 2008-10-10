package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.*;

public class ArrayPlaceholder extends AbstractPlaceholder {
	long[] f_siteIds = new long[4];
	int size = 2;
	
	static int total = 0;
	static final int[] sites = new int[4];
	
	ArrayPlaceholder(Placeholder caller, long siteId) {
		super(caller);
		f_siteIds[0] = caller.f_siteId;
		f_siteIds[1] = siteId;
	}
	
	public TraceNode getNode(final Store.State state) {
		TraceNode n = f_caller == null ? null : f_caller.getNode(state);
		for(int i=0; i<size; i++) {
			n = getNode(state, n, f_siteIds[i]);
		}
		total++;
		sites[size]++;
		if ((total & 0xffff) == 0) {
			//System.err.println(sites+" sites for "+total+" placeholders");
			System.err.println(total+" placeholders");
			for(int i=0; i<sites.length; i++) {
				System.err.println(i+":\t"+sites[i]);
			}
		}
		return n;
	}
	
	private void resize(final int newSize) {
		final int capacity = f_siteIds.length;
		if (newSize > capacity) {
			long[] tmp = new long[capacity];
			System.arraycopy(f_siteIds, 0, tmp, 0, size);
			f_siteIds = tmp;
		}		
	}
	
	@Override
	public ITraceNode pushCallee(long siteId) {				
		final int i = size; 
		resize(i+1);
		f_siteIds[i] = siteId;
		return this;
	}
	
	@Override
	public ITraceNode popParent() {
		size--;
		if (size == 0) {
			return f_caller;
		}		
		return this;
	}
}
