package com.surelogic.flashlight.common.prep;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

import static com.surelogic._flashlight.common.AttributeType.*;
import static com.surelogic._flashlight.common.IdConstants.*;

public final class ScanRawFilePreScan extends DefaultHandler {

	private boolean f_firstTimeEventFound = false;

	final SLProgressMonitor f_monitor;

	public ScanRawFilePreScan(final SLProgressMonitor monitor) {
		assert monitor != null;
		f_monitor = monitor;
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

	private Set<Long> f_singleThreadedStaticFields = new HashSet<Long>();

	/**
	 * Gets if the passed field is a single-threaded static field or not. A
	 * field is considered single-threaded if it is only accessed from one
	 * thread.
	 * 
	 * @param fieldId
	 *            the static field to check.
	 * @return {@code true} if the field is single-threaded, {@code false}
	 *         otherwise.
	 */
	public boolean isSingleThreadedStaticField(final long fieldId) {
		return f_singleThreadedStaticFields.contains(fieldId);
	}

	private static class Pair {
		long f_field;
		long f_receiver;

		Pair(long field, long receiver) {
			f_field = field;
			f_receiver = receiver;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (f_field ^ (f_field >>> 32));
			result = prime * result + (int) (f_receiver ^ (f_receiver >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Pair))
				return false;
			Pair p = (Pair) o;
			return f_field == p.f_field && f_receiver == p.f_receiver;
		}
	}

	private Set<Pair> f_singleThreadedFields = new HashSet<Pair>();

	/**
	 * Gets if the passed field is a single-threaded instance field or not. A
	 * field is considered single-threaded if it is only accessed from one
	 * thread.
	 * 
	 * @param fieldId
	 *            the instance field to check.
	 * @param receiverId
	 *            the receiver the instance field is within.
	 * @return {@code true} if the field is single-threaded, {@code false}
	 *         otherwise.
	 */
	public boolean isThreadedField(final long fieldId, final long receiverId) {
		Pair p = new Pair(fieldId, receiverId);
		return f_singleThreadedFields.contains(p);
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		f_elementCount++;
		/*
		 * Show progress to the user
		 */
		f_monitor.worked(1);
		/*
		 * Check for a user cancel.
		 */
		if (f_monitor.isCanceled())
			throw new SAXException("canceled");

		if ("single-threaded-field".equals(name)) {
			long field = ILLEGAL_FIELD_ID;
			long receiver = -1;
			if (attributes != null) {
				for (int i = 0; i < attributes.getLength(); i++) {
					final String aName = attributes.getQName(i);
					final String aValue = attributes.getValue(i);
					if (FIELD.matches(aName)) {
						field = Long.parseLong(aValue);
					} else if (RECEIVER.matches(aName)) {
						receiver = Long.parseLong(aValue);
					}
				}
			}
			if (field == ILLEGAL_FIELD_ID) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Missing field in single-threaded-field");
				return;
			}
			if (receiver == -1) {
				// static field
				f_singleThreadedStaticFields.add(field);
			} else {
				// instance field
				f_singleThreadedFields.add(new Pair(field, receiver));
			}
		} else if ("time".equals(name)) {
			if (f_firstTimeEventFound) {
				if (attributes != null) {
					for (int i = 0; i < attributes.getLength(); i++) {
						final String aName = attributes.getQName(i);
						if ("nano-time".equals(aName)) {
							long time = Long.parseLong(attributes.getValue(i));
							f_endTime = time;
						}
					}
				}
			} else {
				f_firstTimeEventFound = true;
			}
		}
	}

	@Override
	public void endDocument() throws SAXException {
		if (f_endTime == -1) {
			SLLogger.getLogger().log(Level.SEVERE, "Missing end time element");
		}
	}
}
