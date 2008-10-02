package com.surelogic.flashlight.common.prep;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

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

	private final Set<Long> f_unreferencedObjects = new HashSet<Long>();

	private void newObject(final long id) {
		f_unreferencedObjects.add(id);
	}

	private void useObject(final long id) {
		f_unreferencedObjects.remove(id);
	}

	/**
	 * Returns whether or not this object is referenced.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isUnusedObject(final long id) {
		return f_unreferencedObjects.contains(id);
	}

	/*
	 * Collection of non-static fields accessed by multiple threads and the
	 * objects that contain them, keyed by field
	 */
	private final Map<Long, Set<Long>> f_usedFields = new HashMap<Long, Set<Long>>();
	/*
	 * Collection of static fields accessed by multiple threads
	 */
	private final Set<Long> f_usedStatics = new HashSet<Long>();

	private final Map<Long, Long> f_currentStatics = new HashMap<Long, Long>();
	private final Map<Long, Map<Long, Long>> f_currentFields = new HashMap<Long, Map<Long, Long>>();

	/*
	 * Static field accesses
	 */
	private void useField(final long field, final long thread) {
		final Long _thread = f_currentStatics.get(field);
		if (_thread == null) {
			f_currentStatics.put(field, thread);
		} else {
			if (_thread != thread) {
				f_usedStatics.add(field);
			}
		}
	}

	/*
	 * Field accesses
	 */
	private void useField(final long field, final long thread,
			final long receiver) {
		final Map<Long, Long> objectFields = f_currentFields.get(receiver);
		if (objectFields != null) {
			final Long _thread = objectFields.get(field);
			if (_thread == null) {
				objectFields.put(field, thread);
			} else if (_thread != thread) {
				Set<Long> receivers = f_usedFields.get(field);
				if (receivers == null) {
					receivers = new HashSet<Long>();
					f_usedFields.put(field, receivers);
				}
				receivers.add(receiver);
			}
		} else {
			final Map<Long, Long> map = new HashMap<Long, Long>();
			map.put(field, thread);
			f_currentFields.put(receiver, map);
		}
	}

	private void garbageCollect(final long objectId) {
		f_currentFields.remove(objectId);
	}

	private static final List<String> locks = Arrays.asList(
			"after-intrinsic-lock-acquisition", "after-intrinsic-lock-release",
			"after-intrinsic-lock-wait",
			"after-util-concurrent-lock-acquisition-attempt",
			"after-util-concurrent-lock-release-attempt",
			"before-intrinsic-lock-acquisition", "before-intrinsic-lock-wait",
			"before-util-concurrent-lock-acquisition-attempt");

	@Override
	public void startElement(final String uri, final String localName,
			final String name, final Attributes attributes) throws SAXException {
		f_elementCount++;
		/*
		 * Show progress to the user
		 */
		f_monitor.worked(1);
		/*
		 * Check for a user cancel.
		 */
		if (f_monitor.isCanceled()) {
			throw new SAXException("canceled");
		}

		if ("time".equals(name)) {
			if (f_firstTimeEventFound) {
				if (attributes != null) {
					for (int i = 0; i < attributes.getLength(); i++) {
						final String aName = attributes.getQName(i);
						if ("nano-time".equals(aName)) {
							final long time = Long.parseLong(attributes
									.getValue(i));
							f_endTime = time;
						}
					}
				}
			} else {
				f_firstTimeEventFound = true;
			}
		} else if ("after-trace".equals(name)) {
			// We used the class
			useObject(Long.parseLong(attributes.getValue("in-class")));
		} else if ("field-read".equals(name) || "field-write".equals(name)) {
			final long field = Long.parseLong(attributes.getValue("field"));
			final long thread = Long.parseLong(attributes.getValue("thread"));
			final String rStr = attributes.getValue("receiver");
			if (rStr != null) {
				final long receiver = Long.parseLong(rStr);
				useObject(receiver);
				useField(field, thread, receiver);
			} else {
				useField(field, thread);
			}
			useObject(Long.parseLong(attributes.getValue("in-class")));
			useObject(Long.parseLong(attributes.getValue("thread")));
		} else if (locks.contains(name)) {
			useObject(Long.parseLong(attributes.getValue("thread")));
			useObject(Long.parseLong(attributes.getValue("in-class")));
			useObject(Long.parseLong(attributes.getValue("lock")));
		} else if ("read-write-lock-definition".equals(name)) {
			useObject(Long.parseLong(attributes.getValue("id")));
			useObject(Long.parseLong(attributes.getValue("read-lock-id")));
			useObject(Long.parseLong(attributes.getValue("write-lock-id")));
		} else if ("field-definition".equals(name)) {
			useObject(Long.parseLong(attributes.getValue("type")));
		} else if ("class-definition".equals(name)) {
			newObject(Long.parseLong(attributes.getValue("id")));
		} else if ("thread-definition".equals(name)
				|| "object-definition".equals(name)) {
			newObject(Long.parseLong(attributes.getValue("id")));
			useObject(Long.parseLong(attributes.getValue("type")));
		} else if ("garbage-collected-object".equals(name)) {
			garbageCollect(Long.parseLong(attributes.getValue("id")));
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
		return "Threaded Fields: " + f_usedFields.keySet().size()
				+ "\nThreaded Statics: " + f_usedStatics.size()
				+ "\nUnreferenced Objects: " + f_unreferencedObjects.size();
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

	/**
	 * Returns whether or not this field is accessed by multiple threads for the
	 * given receiver.
	 * 
	 * @param field
	 * @param receiver
	 * @return
	 */
	public boolean isThreadedField(final long field, final long receiver) {
		final Set<Long> receivers = f_usedFields.get(field);
		if (receivers != null) {
			return receivers.contains(receiver);
		}
		return false;
	}

	/**
	 * Returns whether or not this field is accessed by multipleThreads for any
	 * receiver
	 * 
	 * @param id
	 * @return
	 */
	public boolean isThreadedField(final long field) {
		return f_usedFields.get(field) != null;
	}

}
