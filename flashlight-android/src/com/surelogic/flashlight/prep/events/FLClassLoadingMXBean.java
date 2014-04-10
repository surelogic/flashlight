package com.surelogic.flashlight.prep.events;

import java.lang.management.ClassLoadingMXBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class FLClassLoadingMXBean implements ClassLoadingMXBean {
    ClassHandler ch;

    public FLClassLoadingMXBean(ClassHandler ch) {
        super();
        this.ch = ch;
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return ObjectName.getInstance("java.lang:type=ClassLoading");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long getTotalLoadedClassCount() {
        return ch.getTotalLoadedClassCount();
    }

    @Override
    public int getLoadedClassCount() {
        return ch.getLoadedClassCount();
    }

    @Override
    public long getUnloadedClassCount() {
        return ch.getUnloadedClassCount();
    }

    @Override
    public boolean isVerbose() {
        return false;
    }

    @Override
    public void setVerbose(boolean value) {

    }

}
