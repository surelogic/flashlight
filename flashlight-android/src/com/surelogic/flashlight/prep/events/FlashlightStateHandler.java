package com.surelogic.flashlight.prep.events;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.surelogic.common.SLUtility;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class FlashlightStateHandler implements EventHandler {

    private FlashlightEvent fe;
    Timestamp wallClock;
    long startTime;
    long finishTime;
    long latestTime;

    boolean seenTime;

    @Override
    public void handle(Event e) {
        if (e.getEventType() == PrepEvent.FLASHLIGHT) {
            fe = (FlashlightEvent) e;
        } else if (e.getEventType() == PrepEvent.TIME) {
            TimeEvent te = (TimeEvent) e;
            if (seenTime) {
                finishTime = latestTime = te.getNanoTime();
            } else {
                startTime = te.getNanoTime();
                final SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS");
                try {
                    wallClock = new Timestamp(dateFormat.parse(
                            te.getStartTime()).getTime());
                } catch (ParseException e1) {
                    throw new IllegalStateException(e1);
                }
                seenTime = true;
            }
        } else if (e.getEventType() == PrepEvent.CHECKPOINT) {
            CheckpointEvent ce = (CheckpointEvent) e;
            latestTime = ce.getNanoTime();
        }
    }

    public String getVersion() {
        if (fe == null) {
            return null;
        }
        return fe.getVersion();
    }

    public String getRun() {
        if (fe == null) {
            return null;
        }
        return fe.getRun();
    }

    public String getHostname() {
        if (fe == null) {
            return null;
        }
        return fe.getHostname();
    }

    public String getUserName() {
        if (fe == null) {
            return null;
        }
        return fe.getUserName();
    }

    public String getJavaVersion() {
        if (fe == null) {
            return null;
        }
        return fe.getJavaVersion();
    }

    public String getJavaVendor() {
        if (fe == null) {
            return null;
        }
        return fe.getJavaVendor();
    }

    public String getOsName() {
        if (fe == null) {
            return null;
        }
        return fe.getOsName();
    }

    public String getOsArch() {
        if (fe == null) {
            return null;
        }
        return fe.getOsArch();
    }

    public int getMaxMemoryMb() {
        if (fe == null) {
            return -1;
        }
        return fe.getMaxMemoryMb();
    }

    public int getProcessors() {
        if (fe == null) {
            return -1;
        }
        return fe.getProcessors();
    }

    public PrepEvent getEventType() {
        if (fe == null) {
            return null;
        }
        return fe.getEventType();
    }

    public long getStartTime() {
        return wallClock.getTime();
    }

    public long getUptime() {
        return wallClock.getTime() + (latestTime - startTime) / 1000;
    }

    public String getOsVersion() {
        if (fe == null) {
            return null;
        }
        return fe.getOsVersion();
    }

    public Timestamp getTimestamp(final long timeNS) {
        return SLUtility.getWall(wallClock, startTime, timeNS);
    }
}
