package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinimalRefinery extends AbstractRefinery {
	private final AtomicBoolean shutdown = new AtomicBoolean(false);

	private final PostMortemStore store;

	MinimalRefinery(final PostMortemStore store,
			final BlockingQueue<List<? extends IdPhantomReference>> f_gcQueue) {
		super("flashlight-minimal-refinery", f_gcQueue);
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
			List<? extends IdPhantomReference> gcs = gcQueue.poll();
			if (gcs != null) {
				for (IdPhantomReference pr : gcs) {
					PostMortemStore.putInQueue(store.getState(),
							new GarbageCollectedObject(pr));
				}
			} else {
				Thread.yield();
			}
		}
	}

	@Override
	public void requestShutdown() {
		shutdown.set(true);
		this.interrupt();
	}
}
