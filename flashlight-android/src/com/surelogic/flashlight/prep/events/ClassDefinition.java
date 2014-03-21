package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class ClassDefinition implements Event {

    private final long id;
    private final String name;
    private final int mod;
    private final String classType;

    public ClassDefinition(long id, String name, int mod, String classType) {
        this.id = id;
        this.name = name;
        this.mod = mod;
        this.classType = classType;
    }

    @Override
    public PrepEvent type() {
        return PrepEvent.CLASSDEFINITION;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMod() {
        return mod;
    }

    public String getClassType() {
        return classType;
    }

}
