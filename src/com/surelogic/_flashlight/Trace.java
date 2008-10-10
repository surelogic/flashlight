package com.surelogic._flashlight;

abstract class Trace extends WithinThreadEvent {
	Trace(final long siteId, Store.State state) {
		super(siteId, state.thread);
	}
}
