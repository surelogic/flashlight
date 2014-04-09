package com.surelogic.flashlight.prep.events;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Dummy memory pool bean;
 * 
 * @author nathan
 *
 */
public class FLMemoryPoolMXBean implements MemoryPoolMXBean {

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName
                    .getInstance("java.lang:type=MemoryPool,name=heap");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return "heap";
    }

    @Override
    public MemoryType getType() {
        return MemoryType.HEAP;
    }

    @Override
    public MemoryUsage getUsage() {
        return new MemoryUsage(0, 0, 0, 0);
    }

    @Override
    public MemoryUsage getPeakUsage() {
        return new MemoryUsage(0, 0, 0, 0);
    }

    @Override
    public void resetPeakUsage() {

    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String[] getMemoryManagerNames() {
        return new String[] { "undefined" };
    }

    @Override
    public long getUsageThreshold() {
        return 0;
    }

    @Override
    public void setUsageThreshold(long threshold) {

    }

    @Override
    public boolean isUsageThresholdExceeded() {
        return false;
    }

    @Override
    public long getUsageThresholdCount() {
        return 0;
    }

    @Override
    public boolean isUsageThresholdSupported() {
        return false;
    }

    @Override
    public long getCollectionUsageThreshold() {
        return 0;
    }

    @Override
    public void setCollectionUsageThreshold(long threshold) {

    }

    @Override
    public boolean isCollectionUsageThresholdExceeded() {
        return false;
    }

    @Override
    public long getCollectionUsageThresholdCount() {
        return 0;
    }

    @Override
    public MemoryUsage getCollectionUsage() {
        return null;
    }

    @Override
    public boolean isCollectionUsageThresholdSupported() {
        return false;
    }

}
