package com.surelogic.flashlight.prep.events;

public class ReferenceDefinition {

    private final long id;
    private final long type;

    public ReferenceDefinition(long id, long type) {
        super();
        this.id = id;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public long getType() {
        return type;
    }

}
