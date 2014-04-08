package com.surelogic.flashlight.prep.events;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.FlagType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class SaxElemBuilder implements EventBuilder {

    FlashlightEvent fe = new FlashlightEvent();

    @Override
    public Event getEvent(PrepEvent type, PreppedAttributes pa) {
        switch (type) {
        case AFTERINTRINSICLOCKACQUISITION:
        case AFTERINTRINSICLOCKRELEASE:
        case AFTERINTRINSICLOCKWAIT:
        case BEFOREINTRINSICLOCKACQUISITION:
        case BEFOREINTRINSICLOCKWAIT:
            return new IntrinsicLockEvent(type, pa.getLong(AttributeType.TIME),
                    pa.getLong(AttributeType.THREAD),
                    pa.getLong(AttributeType.TRACE),
                    pa.getLong(AttributeType.LOCK),
                    pa.getBoolean(FlagType.THIS_LOCK),
                    pa.getBoolean(FlagType.CLASS_LOCK));
        case AFTERUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            return new JUCLockEvent(type, pa.getLong(AttributeType.TIME),
                    pa.getLong(AttributeType.THREAD),
                    pa.getLong(AttributeType.TRACE),
                    pa.getLong(AttributeType.LOCK),
                    pa.getBoolean(FlagType.GOT_LOCK));
        case AFTERUTILCONCURRENTLOCKRELEASEATTEMPT:
            return new JUCLockEvent(type, pa.getLong(AttributeType.TIME),
                    pa.getLong(AttributeType.THREAD),
                    pa.getLong(AttributeType.TRACE),
                    pa.getLong(AttributeType.LOCK),
                    pa.getBoolean(FlagType.RELEASED_LOCK));
        case BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            return new JUCLockEvent(type, pa.getLong(AttributeType.TIME),
                    pa.getLong(AttributeType.THREAD),
                    pa.getLong(AttributeType.TRACE),
                    pa.getLong(AttributeType.LOCK));
        case CHECKPOINT:
            return new CheckpointEvent(pa.getLong(AttributeType.TIME));
        case CLASSDEFINITION:
            break;
        case ENVIRONMENT:
            fe.setHostname(pa.getString(AttributeType.HOSTNAME));
            fe.setUserName(pa.getString(AttributeType.USER_NAME));
            fe.setJavaVersion(pa.getString(AttributeType.JAVA_VERSION));
            fe.setJavaVendor(pa.getString(AttributeType.JAVA_VENDOR));
            fe.setOsName(pa.getString(AttributeType.OS_NAME));
            fe.setOsArch(pa.getString(AttributeType.OS_ARCH));
            fe.setOsVersion(pa.getString(AttributeType.OS_VERSION));
            fe.setMaxMemoryMb(pa.getInt(AttributeType.MEMORY_MB));
            fe.setProcessors(pa.getInt(AttributeType.CPUS));
            FlashlightEvent tmp = fe;
            fe = null;
            return tmp;
        case FIELDASSIGNMENT:
            break;
        case FIELDDEFINITION:
            break;
        case FIELDREAD:
            return new FieldAccess(type, pa.getLong(AttributeType.FIELD),
                    pa.getLong(AttributeType.TIME),
                    pa.getLong(AttributeType.THREAD),
                    pa.getLong(AttributeType.TRACE),
                    pa.getLong(AttributeType.RECEIVER));
        case FIELDWRITE:
            break;
        case FINAL:
            break;
        case FLASHLIGHT:
            pa.getString(AttributeType.RUN);
            pa.getString(AttributeType.VERSION);
            break;
        case GARBAGECOLLECTEDOBJECT:
            return new GCObject(pa.getLong(AttributeType.ID));
        case HAPPENSBEFORECOLLECTION:
            break;
        case HAPPENSBEFOREOBJECT:
            break;
        case HAPPENSBEFORETHREAD:
            break;
        case INDIRECTACCESS:
            break;
        case OBJECTDEFINITION:
            break;
        case READWRITELOCK:
            break;
        case SELECTEDPACKAGE:
            break;
        case SINGLETHREADEFIELD:
            break;
        case STATICCALLLOCATION:
            return new StaticCallLocation(pa.getLong(AttributeType.ID),
                    pa.getLong(AttributeType.IN_CLASS),
                    pa.getString(AttributeType.LOCATION),
                    pa.getInt(AttributeType.LINE),
                    pa.getString(AttributeType.FILE));
        case THREADDEFINITION:
            break;
        case TIME:
            return new TimeEvent(pa.getLong(AttributeType.NANO_START),
                    pa.getString(AttributeType.WALL_CLOCK));
        case TRACENODE:
            return new TraceNode(pa.getLong(AttributeType.TRACE),
                    pa.getLong(AttributeType.PARENT_ID),
                    pa.getLong(AttributeType.SITE_ID));
        default:
            break;
        }
        return null;
    }
}
