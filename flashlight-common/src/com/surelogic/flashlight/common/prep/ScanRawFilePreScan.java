package com.surelogic.flashlight.common.prep;

import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.carrotsearch.hppc.LongLongMap;
import com.carrotsearch.hppc.LongLongScatterMap;
import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;
import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

public final class ScanRawFilePreScan extends AbstractDataScan {

    private static final int SYNTHETIC = 0x00001000;

    private boolean f_firstTimeEventFound = false;

    public ScanRawFilePreScan(final SLProgressMonitor monitor) {
        super(monitor);
    }

    private long f_elementCount = 0;

    /**
     * Gets the number of XML elements found in the raw file.
     *
     * @return the number of XML elements found in the raw file.
     */
    public long getElementCount() {
        return f_elementCount;
    }

    private long f_endTime = -1;

    /**
     * Gets the <tt>nano-time</tt> value from the final <tt>time</tt> event at
     * the end of the raw data file.
     *
     * @return the <tt>nano-time</tt> value from the final <tt>time</tt> event
     *         at the end of the raw data file.
     */
    public long getEndNanoTime() {
        return f_endTime;
    }

    private final LongSet f_synthetics = new LongScatterSet();

    /**
     * Returns the full set of synthetic fields.
     *
     * @return
     */
    public LongSet getSynthetics() {
        return f_synthetics;
    }

    /**
     * Returns whether or not this field was defined as a synthetic field.
     *
     * @param field
     * @return
     */
    public boolean isSynthetic(final long field) {
        return f_synthetics.contains(field);
    }

    private final LongLongMap f_rwLocks = new LongLongScatterMap();

    /**
     * Returns the lock id, given the locked object. These id's may be the same,
     * but are different in the case of read-write locks.
     *
     * @param object
     * @return
     */
    public long getLockFromObject(final long object) {
        final long lock = f_rwLocks.get(object);
        return lock == 0 ? object : lock;
    }

    /*
     * Collection of static fields accessed by multiple threads
     */
    private final LongSet f_usedStatics = new LongScatterSet();
    /*
     * Field -> Thread
     */
    private final LongLongMap f_currentStatics = new LongLongScatterMap();

    private long f_maxReceiverId;

    public long getMaxReceiverId() {
        return f_maxReceiverId;
    }

    /*
     * Static field accesses
     */
    private void useField(final long field, final long thread) {
        final long _thread = f_currentStatics.get(field);
        if (_thread == 0) {
            f_currentStatics.put(field, thread);
        } else {
            if (_thread != thread) {
                f_usedStatics.add(field);
            }
        }
    }

    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes attributes) throws SAXException {
        f_elementCount++;

        // modified to try and reduce computation overhead)
        if ((f_elementCount & 0x1f) == 0x1f) {
            /*
             * Show progress to the user
             */
            f_monitor.worked(32);

            /*
             * Check for a user cancel.
             */
            if (f_monitor.isCanceled()) {
                throw new SAXException("canceled");
            }
        }
        if (f_elementCount % 1000000 == 0) {
            logState();
        }
        final PreppedAttributes attrs = preprocessAttributes(name, attributes);
        PrepEvent event = PrepEvent.getEvent(name);
        switch (event) {
        case FIELDREAD:
        case FIELDWRITE:
            final long field = attrs.getLong(AttributeType.FIELD);
            final long thread = attrs.getThreadId();
            final long receiver = attrs.getLong(AttributeType.RECEIVER);
            if (receiver == IdConstants.ILLEGAL_RECEIVER_ID) {
                useField(field, thread);
            }
            break;
        case READWRITELOCK:
            final long id = attrs.getLong(AttributeType.ID);
            final long rLock = attrs.getLong(AttributeType.READ_LOCK_ID);
            final long wLock = attrs.getLong(AttributeType.WRITE_LOCK_ID);
            f_rwLocks.put(rLock, id);
            f_rwLocks.put(wLock, id);
            break;
        case FIELDDEFINITION:
            final int mod = attrs.getInt(AttributeType.MODIFIER);
            if ((mod & SYNTHETIC) != 0) {
                f_synthetics.add(attrs.getLong(AttributeType.ID));
            }
            break;
        case OBJECTDEFINITION:
        case THREADDEFINITION:
        case CLASSDEFINITION:
            final long classId = attrs.getLong(AttributeType.ID);
            f_maxReceiverId = Math.max(f_maxReceiverId, classId);
            break;
        case TIME:
            if (f_firstTimeEventFound) {
                f_endTime = attrs.getEventTime();
            } else {
                f_firstTimeEventFound = true;
            }
            break;
        case CHECKPOINT:
            f_endTime = attrs.getEventTime();
            break;
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
        case ENVIRONMENT:
            break;
        case FIELDASSIGNMENT:
            break;
        case FINAL:
            break;
        case FLASHLIGHT:
            break;
        case GARBAGECOLLECTEDOBJECT:
            break;
        case HAPPENSBEFORETHREAD:
            break;
        case HAPPENSBEFOREOBJECT:
            break;
        case HAPPENSBEFORECOLLECTION:
            break;
        case HAPPENSBEFOREEXEC:
            break;
        case INDIRECTACCESS:
            break;
        case SELECTEDPACKAGE:
            break;
        case SINGLETHREADEFIELD:
            break;
        case STATICCALLLOCATION:
            break;
        case TRACENODE:
            break;
        default:
            break;
        }
    }

    private void logState() {
        SLLogger.getLoggerFor(ScanRawFilePreScan.class).fine(
                "f_rwLocks " + f_rwLocks.size() + "\n\tf_usedStatics "
                        + f_usedStatics.size() + "\n\tf_currentStatics "
                        + f_currentStatics.size());
    }

    @Override
    public void endDocument() throws SAXException {
        if (f_endTime == -1) {
            SLLogger.getLogger().log(Level.SEVERE, "Missing end time element");
        }
        logState();
    }

    @Override
    public String toString() {
        return "\nThreaded Statics: " + f_usedStatics.size();

    }

    /**
     * Returns whether or not this field is accessed by multiple threads.
     *
     * @param field
     * @return
     */
    public boolean isThreadedStaticField(final long field) {
        return f_usedStatics.contains(field);
    }

}
