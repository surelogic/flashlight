package com.surelogic._flashlight;

import java.util.concurrent.atomic.AtomicBoolean;

public class MinimalRefinery extends AbstractRefinery {
	private final AtomicBoolean shutdown = new AtomicBoolean(false);
	
	MinimalRefinery() {
		super("flashlight-minimal-refinery");
	}

	/**
	 * 1. Needs to create events for garbage-collected objects
	 * 2. Store.shutdown() flushes thread-local event queues
	 */
	@Override
	public void run() {
		Store.flashlightThread();
		while (shutdown.get()) {
			IdPhantomReference pr = Phantom.get();
			if (pr != null) {
				Store.putInQueue(Store.getRawQueue(), new GarbageCollectedObject(pr));
			}
		}
	}
	
	@Override
	public void requestShutdown() {
		shutdown.set(true);
		this.interrupt();
	}
}
