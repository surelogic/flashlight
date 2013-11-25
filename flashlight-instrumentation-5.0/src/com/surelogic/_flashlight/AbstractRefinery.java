package com.surelogic._flashlight;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractRefinery extends Thread {
    AbstractRefinery(final String name,
            final BlockingQueue<List<? extends IdPhantomReference>> gcQueue) {
        super(name);
        this.gcQueue = gcQueue;
    }

    protected final BlockingQueue<List<? extends IdPhantomReference>> gcQueue;

    @Override
    public abstract void run();

    public void requestShutdown() {
        // Does nothing here
    }

}
