package com.surelogic.flashlight.prep.events;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLMemoryMXBean implements MemoryMXBean {

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName.getInstance(ManagementFactory.MEMORY_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getObjectPendingFinalizationCount() {
        return 0;
    }

    @Override
    public MemoryUsage getHeapMemoryUsage() {
        return new MemoryUsage(0, 0, 0, 0);
    }

    @Override
    public MemoryUsage getNonHeapMemoryUsage() {
        return new MemoryUsage(0, 0, 0, 0);
    }

    @Override
    public boolean isVerbose() {
        return false;
    }

    @Override
    public void setVerbose(boolean value) {

    }

    @Override
    public void gc() {

    }

}
