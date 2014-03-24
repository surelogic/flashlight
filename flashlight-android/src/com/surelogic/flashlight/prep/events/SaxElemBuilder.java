package com.surelogic.flashlight.prep.events;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.FlagType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class SaxElemBuilder implements EventBuilder {

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
            break;
        case CLASSDEFINITION:
            break;
        case ENVIRONMENT:
            break;
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
            break;
        case THREADDEFINITION:
            break;
        case TIME:
            break;
        case TRACENODE:
            break;
        default:
            break;

        }
        return null;
    }
}
