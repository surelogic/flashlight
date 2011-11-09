package com.surelogic._flashlight;

public class CheckpointEvent extends TimedEvent {

    CheckpointEvent(final long nanoTime) {
        super(nanoTime);
    }

    @Override
    void accept(final EventVisitor v) {
        v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("<checkpoint");
        addNanoTime(b);
        b.append("/>");
        return b.toString();
    }
}
