package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class ObjectDefinition extends ReferenceDefinition implements Event {

    public ObjectDefinition(long id, long type) {
        super(id, type);
    }

    @Override
    public PrepEvent getEventType() {
        return PrepEvent.OBJECTDEFINITION;
    }

}
