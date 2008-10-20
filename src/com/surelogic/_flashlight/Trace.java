package com.surelogic._flashlight;

abstract class Trace extends WithinThreadEvent {
	private final long f_siteId;
	
	public long getSiteId() {
		return f_siteId;
	}
	
	Trace(final long siteId, Store.State state) {
		super(state.thread);
		f_siteId = siteId;
	}
	
	@Override
	protected void addThread(final StringBuilder b) {
		super.addThread(b);
		Entities.addAttribute("site", f_siteId, b);
	}
}

