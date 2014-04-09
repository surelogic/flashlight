package com.surelogic.flashlight.prep.events;

import java.lang.management.GarbageCollectorMXBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLGarbageCollectorMXBean implements GarbageCollectorMXBean {

    @Override
    public String getName() {
        return "main";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String[] getMemoryPoolNames() {
        return new String[] { "heap" };
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName
                    .getInstance("java.lang:type=GarbageCollector,name=main");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long getCollectionCount() {
        return 0;
    }

    @Override
    public long getCollectionTime() {
        return 0;
    }

}
