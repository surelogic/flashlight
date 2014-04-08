package com.surelogic.flashlight.prep.events;

import com.surelogic.flashlight.common.prep.PrepEvent;

public class FlashlightEvent implements Event {

    private String version;
    private String run;
    private String hostname;
    private String userName;
    private String javaVersion;
    private String javaVendor;
    private String osName;
    private String osArch;
    private String osVersion;

    private int maxMemoryMb;
    private int processors;

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setJavaVendor(String javaVendor) {
        this.javaVendor = javaVendor;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public void setProcessors(int processors) {
        this.processors = processors;
    }

    public String getVersion() {
        return version;
    }

    public String getRun() {
        return run;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUserName() {
        return userName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsArch() {
        return osArch;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public int getProcessors() {
        return processors;
    }

    @Override
    public PrepEvent getEventType() {
        return PrepEvent.FLASHLIGHT;
    }

}
