package com.surelogic.flashlight.prep.events;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLCompilationMXBean implements CompilationMXBean {

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName
                    .getInstance(ManagementFactory.COMPILATION_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public boolean isCompilationTimeMonitoringSupported() {
        return false;
    }

    @Override
    public long getTotalCompilationTime() {
        return 0;
    }

}
