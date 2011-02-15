package com.surelogic._flashlight;

import java.util.concurrent.atomic.AtomicBoolean;

public class MinimalRefinery extends AbstractRefinery {
	private final AtomicBoolean shutdown = new AtomicBoolean(false);

	private final PostMortemStore store;

	MinimalRefinery(final PostMortemStore store) {
		super("flashlight-minimal-refinery");
		this.store = store;
	}

	/**
	 * 1. Needs to create events for garbage-collected objects 2.
	 * Store.shutdown() flushes thread-local event queues
	 */
	@Override
	public void run() {
		Store.flashlightThread();
		while (shutdown.get()) {
			IdPhantomReference pr = Phantom.get();
			if (pr != null) {
				store.putInQueue(store.getState(), new GarbageCollectedObject(
						pr));
			}
		}
	}

	@Override
	public void requestShutdown() {
		shutdown.set(true);
		this.interrupt();
	}
}
