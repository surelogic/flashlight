package com.surelogic.flashlight.prep.events;

import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.flashlight.common.prep.PrepEvent;

public class SaxElemBuilder implements EventBuilder {

    @Override
    public Event getEvent(PrepEvent type, PreppedAttributes pa) {
        switch (type) {
        case AFTERINTRINSICLOCKACQUISITION:
            break;
        case AFTERINTRINSICLOCKRELEASE:
            break;
        case AFTERINTRINSICLOCKWAIT:
            break;
        case AFTERUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            break;
        case AFTERUTILCONCURRENTLOCKRELEASEATTEMPT:
            break;
        case BEFOREINTRINSICLOCKACQUISITION:
            break;
        case BEFOREINTRINSICLOCKWAIT:
            break;
        case BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT:
            break;
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
            break;
        case FIELDWRITE:
            break;
        case FINAL:
            break;
        case FLASHLIGHT:
            break;
        case GARBAGECOLLECTEDOBJECT:
            break;
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
