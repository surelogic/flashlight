package com.surelogic.flashlight.common.prep;

import gnu.trove.TLongHashSet;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectProcedure;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

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

	private final TLongHashSet f_referencedObjects = new TLongHashSet();

	private void newObject(final long id) {
		// f_unreferencedObjects.add(id);
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

	private final TLongLongHashMap f_rwLocks = new TLongLongHashMap();

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
	 * Collection of non-static fields accessed by multiple threads and the
	 * objects that contain them, keyed by field
	 * 
	 * Field -> Receivers
	 */
	private final TLongObjectHashMap<TLongHashSet> f_usedFields = new TLongObjectHashMap<TLongHashSet>();
	/*
	 * Collection of static fields accessed by multiple threads
	 */
	private final TLongHashSet f_usedStatics = new TLongHashSet();
	/*
	 * Field -> Thread
	 */
	private final TLongLongHashMap f_currentStatics = new TLongLongHashMap();
	/*
	 * Receiver -> Field -> Thread
	 */
	private final TLongObjectHashMap<TLongLongHashMap> f_currentFields = new TLongObjectHashMap<TLongLongHashMap>();

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

	/*
	 * Field accesses
	 */
	private void useField(final long field, final long thread,
			final long receiver) {
		final TLongLongHashMap objectFields = f_currentFields.get(receiver);
		if (objectFields != null) {
			final long _thread = objectFields.get(field);
			if (_thread == 0) {
				objectFields.put(field, thread);
			} else if (_thread != thread) {
				TLongHashSet receivers = f_usedFields.get(field);
				if (receivers == null) {
					receivers = new TLongHashSet();
					f_usedFields.put(field, receivers);
				}
				receivers.add(receiver);
			}
		} else {
			final TLongLongHashMap map = new TLongLongHashMap();
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
		if ("time".equals(name)) {
			if (f_firstTimeEventFound) {
				f_endTime = attrs.getEventTime();
			} else {
				f_firstTimeEventFound = true;
			}
		} else if ("after-trace".equals(name)) {
			// We used the class
			useObject(attrs.getLong(AttributeType.IN_CLASS));
		} else if ("field-read".equals(name) || "field-write".equals(name)) {
			final long field = attrs.getLong(AttributeType.FIELD);
			final long thread = attrs.getThreadId();
			final long receiver = attrs.getLong(AttributeType.RECEIVER);
			if (receiver != IdConstants.ILLEGAL_RECEIVER_ID) {
				useObject(receiver);
				useField(field, thread, receiver);
			} else {
				useField(field, thread);
			}
			useObject(thread);
		} else if ("indirect-access".equals(name)) {
			useObject(attrs.getLong(AttributeType.RECEIVER));
		} else if (locks.contains(name)) {
			useObject(attrs.getThreadId());
			useObject(attrs.getLockObjectId());
		} else if ("read-write-lock-definition".equals(name)) {
			final long id = attrs.getLong(AttributeType.ID);
			final long rLock = attrs.getLong(AttributeType.READ_LOCK_ID);
			final long wLock = attrs.getLong(AttributeType.WRITE_LOCK_ID);
			f_rwLocks.put(rLock, id);
			f_rwLocks.put(wLock, id);
			useObject(id);
			useObject(rLock);
			useObject(wLock);
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

	private void logState() {
		SLLogger.getLoggerFor(ScanRawFilePreScan.class).info(
				"f_referencedObjects: " + f_referencedObjects.size()
						+ "\n\tf_rwLocks " + f_rwLocks.size()
						+ "\n\tf_usedFields " + nestedSum1(f_usedFields)
						+ "\n\tf_usedStatics " + f_usedStatics.size()
						+ "\n\tf_currentStatics " + f_currentStatics.size()
						+ "\n\tf_currentFields " + nestedSum(f_currentFields));
	}

	static class Sum1 implements TObjectProcedure<TLongHashSet> {
		int sum;

		public boolean execute(final TLongHashSet object) {
			sum += object.size();
			return true;
		}

	}

	private String nestedSum1(final TLongObjectHashMap<TLongHashSet> fields) {
		final Sum1 sum = new Sum1();
		fields.forEachValue(sum);
		return Integer.toString(sum.sum);
	}

	static class Sum implements TObjectProcedure<TLongLongHashMap> {
		int sum;

		public boolean execute(final TLongLongHashMap object) {
			sum += object.size();
			return true;
		}

	}

	private String nestedSum(final TLongObjectHashMap<TLongLongHashMap> fields) {
		final Sum sum = new Sum();
		fields.forEachValue(sum);
		return Integer.toString(sum.sum);
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
		final TLongHashSet receivers = f_usedFields.get(field);
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
