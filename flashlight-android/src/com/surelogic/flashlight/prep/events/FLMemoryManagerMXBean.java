package com.surelogic.flashlight.prep.events;

import java.lang.management.MemoryManagerMXBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Dummy bean
 *
 * @author nathan
 *
 */
public class FLMemoryManagerMXBean implements MemoryManagerMXBean {

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName
                    .getInstance("java.lang:type=MemoryManager,name=undefined");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return "undefined";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String[] getMemoryPoolNames() {
        return new String[] { "heap" };
    }

}
