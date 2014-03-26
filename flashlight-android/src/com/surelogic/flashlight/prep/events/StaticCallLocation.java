package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class StaticCallLocation implements Event {

    private final long id;
    private final long inClass;
    private final String location;
    private final int line;
    private final String file;

    StaticCallLocation(long id, long inClass, String location, int line,
            String file) {
        this.id = id;
        this.inClass = inClass;
        this.location = location;
        this.line = line;
        this.file = file;
    }

    @Override
    public PrepEvent getEventType() {
        return PrepEvent.STATICCALLLOCATION;
    }

    public long getId() {
        return id;
    }

    public long getInClass() {
        return inClass;
    }

    public String getLocation() {
        return location;
    }

    public int getLine() {
        return line;
    }

    public String getFile() {
        return file;
    }

}
