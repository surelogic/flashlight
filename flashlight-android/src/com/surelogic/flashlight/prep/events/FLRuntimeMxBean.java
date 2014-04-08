package com.surelogic.flashlight.prep.events;

import java.lang.management.RuntimeMXBean;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLRuntimeMxBean implements RuntimeMXBean {

    FlashlightStateHandler handler;

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName.getInstance("java.lang:type=Runtime");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getName() {
        return handler.getRun();
    }

    @Override
    public String getVmName() {
        return handler.getJavaVendor();
    }

    @Override
    public String getVmVendor() {
        return handler.getJavaVendor();
    }

    @Override
    public String getVmVersion() {
        return handler.getJavaVersion();
    }

    @Override
    public String getSpecName() {
        return handler.getOsName();
    }

    @Override
    public String getSpecVendor() {
        return handler.getJavaVendor();
    }

    @Override
    public String getSpecVersion() {
        return handler.getJavaVersion();
    }

    @Override
    public String getManagementSpecVersion() {
        return null;
    }

    @Override
    public String getClassPath() {
        return null;
    }

    @Override
    public String getLibraryPath() {
        return null;
    }

    @Override
    public boolean isBootClassPathSupported() {
        return false;
    }

    @Override
    public String getBootClassPath() {
        return null;
    }

    @Override
    public List<String> getInputArguments() {
        return Collections.emptyList();
    }

    @Override
    public long getUptime() {
        return handler.getUptime();
    }

    @Override
    public long getStartTime() {
        return handler.getStartTime();
    }

    @Override
    public Map<String, String> getSystemProperties() {
        return Collections.emptyMap();
    }

}
