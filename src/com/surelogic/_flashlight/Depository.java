package com.surelogic._flashlight;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Takes events from the out queue and persists them according to an output
 * strategy.
 */
final class Depository extends Thread {

	private final BlockingQueue<Event> f_outQueue;

	private volatile EventVisitor f_outputStrategy;

	Depository(final BlockingQueue<Event> outQueue,
			final EventVisitor outputStrategy) {
		super("flashlight-depository");
		assert outQueue != null;
		f_outQueue = outQueue;
		assert outputStrategy != null;
		f_outputStrategy = outputStrategy;
	}

	private boolean f_finished = false;

	private final AtomicLong f_outputCount = new AtomicLong();

	@Override
	public void run() {
		Store.flashlightThread();

		while (!f_finished) {
			try {
				Event e = f_outQueue.take();
				if (e == FinalEvent.FINAL_EVENT)
					f_finished = true;
				e.accept(f_outputStrategy);
				f_outputCount.incrementAndGet();
			} catch (InterruptedException e) {
				Store.logAProblem("depository was interrupted...a bug");
			}
		}
		Store.log("depository flushed (" + f_outputCount.get()
				+ " events(s) output)");
	}

	/**
	 * Sets the output strategy used by the Depository. This method is intended
	 * for test code use only because the {@link Store} configures the output
	 * strategy for this thread upon construction. Tests, however, may want to
	 * avoid output and/or simply count events.
	 * 
	 * @param outputStrategy
	 *            an output strategy.
	 */
	void setOutputStrategy(final EventVisitor outputStrategy) {
		if (outputStrategy == null)
			throw new IllegalArgumentException(
					"outputStrategy must be non-null");
		f_outputStrategy = outputStrategy;
	}
}
