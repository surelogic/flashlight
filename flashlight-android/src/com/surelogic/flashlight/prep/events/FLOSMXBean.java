package com.surelogic.flashlight.prep.events;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLOSMXBean implements OperatingSystemMXBean {
    FlashlightStateHandler fe;

    public FLOSMXBean(FlashlightStateHandler fe) {
        super();
        this.fe = fe;
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName
                    .getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return fe.getOsName();
    }

    @Override
    public String getArch() {
        return fe.getOsArch();
    }

    @Override
    public String getVersion() {
        return fe.getOsVersion();
    }

    @Override
    public int getAvailableProcessors() {
        return fe.getProcessors();
    }

    @Override
    public double getSystemLoadAverage() {
        return 0;
    }

}
