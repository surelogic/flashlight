package com.surelogic.flashlight.common.prep;

import java.util.*;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic._flashlight.common.*;

public final class ScanRawFilePreScan extends AbstractDataScan {

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

	private final LongSet f_referencedObjects = new LongSet();
	
	private void newObject(final long id) {
		//f_unreferencedObjects.add(id);
		// What is there to do?
	}

	private void useObject(final long id) {
		f_referencedObjects.add(id);
	}

	/**
	 * Returns whether or not this object might be referenced.
	 * 
	 * @param id
	 * @return
	 */
	public boolean couldBeReferencedObject(final long id) {
		return f_referencedObjects.contains(id);
	}

	/*
	 * Collection of non-static fields accessed by multiple threads and the
	 * objects that contain them, keyed by field
	 */
	private final LongMap<ILongSet> f_usedFields = new LongMap<ILongSet>();
	/*
	 * Collection of static fields accessed by multiple threads
	 */
	private final ILongSet f_usedStatics = new LongSet();

	private final LongMap<Long> f_currentStatics = new LongMap<Long>();
	private final LongMap<LongMap<Long>> f_currentFields = new LongMap<LongMap<Long>>();

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
		final LongMap<Long> objectFields = f_currentFields.get(receiver);
		if (objectFields != null) {
			final Long _thread = objectFields.get(field);
			if (_thread == null) {
				objectFields.put(field, thread);
			} else if (_thread != thread) {
				ILongSet receivers = f_usedFields.get(field);
				if (receivers == null) {
					receivers = new LongSet();
					f_usedFields.put(field, receivers);
				}
				receivers.add(receiver);
			}
		} else {
			final LongMap<Long> map = new LongMap<Long>();
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

		// modified to try and reduce computation overhead)
		if ((f_elementCount & 0x7) == 7) {
			/*
			 * Show progress to the user
			 */
			f_monitor.worked(8);
			
			/*
			 * Check for a user cancel.
			 */
			if (f_monitor.isCanceled()) {
				throw new SAXException("canceled");
			}
		}

		EventType e             = EventType.findByLabel(name);
		PreppedAttributes attrs = preprocessAttributes(e, attributes);
		if ("time".equals(name)) {
			if (f_firstTimeEventFound) {
				f_endTime = attrs.getLong(AttributeType.TIME);
			} else {
				f_firstTimeEventFound = true;
			}
		} else if ("after-trace".equals(name)) {
			// We used the class
			useObject(attrs.getLong(AttributeType.IN_CLASS));
		} else if ("field-read".equals(name) || "field-write".equals(name)) {
			final long field  = attrs.getLong(AttributeType.FIELD);
			final long thread = attrs.getLong(AttributeType.THREAD);
			final long receiver = attrs.getLong(AttributeType.RECEIVER);
			if (receiver != IdConstants.ILLEGAL_RECEIVER_ID) {
				useObject(receiver);
				useField(field, thread, receiver);
			} else {
				useField(field, thread);
			}
			useObject(attrs.getLong(AttributeType.IN_CLASS));
			useObject(thread);
		} else if (locks.contains(name)) {
			useObject(attrs.getLong(AttributeType.THREAD));
			useObject(attrs.getLong(AttributeType.IN_CLASS));
			useObject(attrs.getLong(AttributeType.LOCK));
		} else if ("read-write-lock-definition".equals(name)) {
			useObject(attrs.getLong(AttributeType.ID));
			useObject(attrs.getLong(AttributeType.READ_LOCK_ID));
			useObject(attrs.getLong(AttributeType.WRITE_LOCK_ID));
		} else if ("field-definition".equals(name)) {
			useObject(attrs.getLong(AttributeType.TYPE));
		} else if ("class-definition".equals(name)) {
			newObject(attrs.getLong(AttributeType.ID));
		} else if ("thread-definition".equals(name)
				|| "object-definition".equals(name)) {
			newObject(attrs.getLong(AttributeType.ID));
			useObject(attrs.getLong(AttributeType.TYPE));
		} else if ("garbage-collected-object".equals(name)) {
			garbageCollect(attrs.getLong(AttributeType.ID));
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
		return "Threaded Fields: " + f_usedFields.size()
				+ "\nThreaded Statics: " + f_usedStatics.size()
				+ "\nReferenced Objects: " + f_referencedObjects.size();
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
		final ILongSet receivers = f_usedFields.get(field);
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
