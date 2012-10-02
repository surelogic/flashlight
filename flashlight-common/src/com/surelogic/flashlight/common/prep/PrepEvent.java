package com.surelogic.flashlight.common.prep;

import java.util.HashMap;
import java.util.Map;

public enum PrepEvent {
    AFTERINTRINSICLOCKACQUISITION("after-intrinsic-lock-acquisition"), AFTERINTRINSICLOCKRELEASE(
            "after-intrinsic-lock-release"), AFTERINTRINSICLOCKWAIT(
            "after-intrinsic-lock-wait"), AFTERUTILCONCURRENTLOCKACQUISITIONATTEMPT(
            "after-util-concurrent-lock-acquisition-attempt"), AFTERUTILCONCURRENTLOCKRELEASEATTEMPT(
            "after-util-concurrent-lock-release-attempt"), BEFOREINTRINSICLOCKACQUISITION(
            "before-intrinsic-lock-acquisition"), BEFOREINTRINSICLOCKWAIT(
            "before-intrinsic-lock-wait"), BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT(
            "before-util-concurrent-lock-acquisition-attempt"), CLASSDEFINITION(
            "class-definition"), ENVIRONMENT("environment"), FIELDASSIGNMENT(
            "field-assignment"), FIELDDEFINITION("field-definition"), FIELDREAD(
            "field-read"), FIELDWRITE("field-write"), FINAL("final"), FLASHLIGHT(
            "flashlight"), GARBAGECOLLECTEDOBJECT("garbage-collected-object"), INDIRECTACCESS(
            "indirect-access"), OBJECTDEFINITION("object-definition"), READWRITELOCK(
            "read-write-lock-definition"), SELECTEDPACKAGE("selected-package"), STATICCALLLOCATION(
            "static-call-location"), SINGLETHREADEFIELD("single-threaded-field"), THREADDEFINITION(
            "thread-definition"), TIME("time"), TRACENODE("trace-node"), CHECKPOINT(
            "checkpoint"), HAPPENSBEFORE("happens-before");

    static Map<String, PrepEvent> map = new HashMap<String, PrepEvent>();

    static {
        for (final PrepEvent p : PrepEvent.values()) {
            map.put(p.xml, p);
        }
    }

    public static PrepEvent getEvent(final String xml) {
        final PrepEvent e = map.get(xml);
        if (e == null) {
            throw new IllegalArgumentException(xml
                    + " is not a recognized tag.");
        }
        return e;
    }

    PrepEvent(final String xmlName) {
        xml = xmlName;
    }

    private String xml;

    public String getXmlName() {
        return xml;
    }
}
