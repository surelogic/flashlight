package com.surelogic._flashlight;

public class CheckpointEvent extends Event {

    @Override
    void accept(final EventVisitor v) {
        v.visit(this);
    }

}
