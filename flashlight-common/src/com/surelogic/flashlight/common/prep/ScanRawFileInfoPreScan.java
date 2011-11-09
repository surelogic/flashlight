package com.surelogic.flashlight.common.prep;

import gnu.trove.TLongProcedure;
import gnu.trove.TObjectLongHashMap;

import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

public class ScanRawFileInfoPreScan extends AbstractDataScan {

    private boolean f_firstTimeEventFound = false;
    private long f_maxReceiverId;
    private final TObjectLongHashMap<PrepEvent> f_elementCounts = new TObjectLongHashMap<PrepEvent>();
    private long f_endTime = -1;

    public ScanRawFileInfoPreScan(final SLProgressMonitor monitor) {
        super(monitor);
        for (final PrepEvent e : PrepEvent.values()) {
            f_elementCounts.put(e, 0L);
        }
    }

    /*
     * Summation helper procedure
     */
    private static class Summation implements TLongProcedure {
        long count = 0L;

        @Override
        public boolean execute(final long value) {
            count += value;
            return true;
        }
    }

    /**
     * Gets the number of XML elements found in the raw file.
     * 
     * @return the number of XML elements found in the raw file.
     */
    public long getElementCount() {
        final Summation s = new Summation();
        f_elementCounts.forEachValue(s);
        return s.count;
    }

    /**
     * Gets the number of XML elements of the given type found in the raw file.
     * 
     * @param e
     * @return
     */

    public long getElementCount(final PrepEvent e) {
        return f_elementCounts.get(e);
    }

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

    /**
     * Gets the maximum object id reported by a thread, object, or class
     * definition.
     * 
     * @return
     */
    public long getMaxReceiverId() {
        return f_maxReceiverId;
    }

    int f_counter = 0;

    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes attributes) throws SAXException {
        f_counter++;

        // modified to try and reduce computation overhead)
        if ((f_counter & 0x1f) == 0x1f) {
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
        final PrepEvent e = PrepEvent.getEvent(name);
        f_elementCounts.increment(e);
        final PreppedAttributes attrs = preprocessAttributes(name, attributes);
        switch (e) {
        case CHECKPOINT:
        case TIME:
            if (f_firstTimeEventFound) {
                f_endTime = attrs.getEventTime();
            } else {
                f_firstTimeEventFound = true;
            }
            break;
        case OBJECTDEFINITION:
        case THREADDEFINITION:
        case CLASSDEFINITION:
            final long id = attrs.getLong(AttributeType.ID);
            f_maxReceiverId = Math.max(f_maxReceiverId, id);
            break;
        default:
            break;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (f_endTime == -1) {
            SLLogger.getLogger().log(Level.SEVERE, "Missing end time element");
        }
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("Scan statistics:\n");
        for (final PrepEvent e : PrepEvent.values()) {
            b.append("\t");
            b.append(e.toString());
            b.append(": ");
            b.append(f_elementCounts.get(e));
            b.append("\n");
        }
        return b.toString();
    }
}
