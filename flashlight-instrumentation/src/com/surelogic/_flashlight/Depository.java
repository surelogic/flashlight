package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Takes events from the out queue and persists them according to an output
 * strategy.
 */
final class Depository extends Thread {

    private final RunConf conf;

    private final BlockingQueue<List<Event>> f_outQueue;

    private volatile EventVisitor f_outputStrategy;

    Depository(final RunConf conf, final BlockingQueue<List<Event>> outQueue,
            final EventVisitor outputStrategy) {
        super("flashlight-depository");
        assert outQueue != null;
        f_outQueue = outQueue;
        assert outputStrategy != null;
        f_outputStrategy = outputStrategy;
        this.conf = conf;
    }

    private boolean f_finished = false;

    private long f_outputCount = 0;

    @Override
    public void run() {
        Store.flashlightThread();
        conf.log("Depository started.");
        while (!f_finished) {
            try {
                final List<Event> buf = f_outQueue.take();
                for (final Event e : buf) {
                    if (e == null) {
                        continue;
                    }
                    if (e == FinalEvent.FINAL_EVENT) {
                        conf.log("Final Event detected in depository.");
                        f_finished = true;
                    }
                    e.accept(f_outputStrategy);
                    f_outputCount++;
                }
                buf.clear();
            } catch (final InterruptedException e) {
                conf.logAProblem("depository was interrupted...a bug");
            }
        }
        f_outputStrategy.flush();
        conf.log("depository flushed (" + f_outputCount + " events(s) output)");

        if (StoreConfiguration.debugOn()) {
            f_outputStrategy.printStats();
        }
    }

}
