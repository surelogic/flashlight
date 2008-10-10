package com.surelogic._flashlight.trace;

import com.surelogic._flashlight.*;

public class PairPlaceholder extends AbstractPlaceholder {
	long f_siteId1;
	long f_siteId2;
	int size = 1;
	
	PairPlaceholder(ITraceNode caller, long siteId) {
		super(caller);
		f_siteId1 = siteId;
	}
	
	public TraceNode getNode(final Store.State state) {
		TraceNode n = f_caller == null ? null : f_caller.getNode(state);
		if (size > 0) {
			n = getNode(state, n, f_siteId1);
		}
		if (size > 1) {
			n = getNode(state, n, f_siteId2);
		}
		return n;
	}
	
	@Override
	public ITraceNode pushCallee(long siteId) {			
		switch (size) {
		case 0:
			f_siteId1 = siteId;
			break;
		case 1:
			f_siteId2 = siteId;
			break;
		case 2:
			return new PairPlaceholder(this, siteId);
		default:
			throw new IllegalArgumentException("Bad size: "+size);
		}
		size++;
		return this;
	}
	
	@Override
	public ITraceNode popParent() {
		switch (size) {
		case 0:
			return f_caller.popParent();
		case 1:
		case 2:
			size--;
			return this;
		default:
			throw new IllegalArgumentException("Bad size: "+size);
		}
	}
}
