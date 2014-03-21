package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class ThreadDefinition extends ReferenceDefinition implements Event {

    @Override
    public PrepEvent type() {
        return PrepEvent.THREADDEFINITION;
    }

    private final String name;

    public ThreadDefinition(long id, long type, String name) {
        super(id, type);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
