package com.surelogic.flashlight.common.prep;

import gnu.trove.TLongHashSet;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectProcedure;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

public class ScanRawFileFieldsPreScan extends AbstractDataScan {

	private long f_elementCount = 0;
	private final long f_start;
	private final long f_end;

	private final TLongHashSet f_referencedObjects = new TLongHashSet();

	/*
	 * Collection of non-static fields accessed by multiple threads and the
	 * objects that contain them, keyed by field
	 */
	private final TLongObjectHashMap<TLongHashSet> f_usedFields = new TLongObjectHashMap<TLongHashSet>();
	private final TLongObjectHashMap<TLongLongHashMap> f_currentFields = new TLongObjectHashMap<TLongLongHashMap>();
	private final TLongHashSet f_synthetics;

	private void useObject(final long id) {
		if (f_start <= id && id <= f_end) {
			f_referencedObjects.add(id);
		}
	}

	public ScanRawFileFieldsPreScan(final SLProgressMonitor monitor,
			final TLongHashSet synthetics, final long start, final long end) {
		super(monitor);
		f_start = start;
		f_end = end;
		f_synthetics = synthetics;
	}

	/*
	 * Field accesses
	 */
	private void useField(final long field, final long thread,
			final long receiver) {
		if (f_start <= receiver && receiver <= f_end) {
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
	}

	private void garbageCollect(final long objectId) {
		f_currentFields.remove(objectId);
	}

	@SuppressWarnings("fallthrough")
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
		final PrepEvent e = PrepEvent.getEvent(name);
		switch (e) {
		case FIELDREAD:
		case FIELDWRITE:
			final long field = attrs.getLong(AttributeType.FIELD);
			final long thread = attrs.getThreadId();
			final long receiver = attrs.getLong(AttributeType.RECEIVER);
			if (receiver != IdConstants.ILLEGAL_RECEIVER_ID) {
				useField(field, thread, receiver);
				useObject(receiver);
			}
			useObject(thread);
			break;
		case GARBAGECOLLECTEDOBJECT:
			garbageCollect(attrs.getLong(AttributeType.ID));
			break;
		case INDIRECTACCESS:
			useObject(attrs.getLong(AttributeType.RECEIVER));
			break;
		case AFTERINTRINSICLOCKACQUISITION:
		case AFTERINTRINSICLOCKRELEASE:
		case AFTERINTRINSICLOCKWAIT:
		case AFTERUTILCONCURRENTLOCKACQUISITIONATTEMPT:
		case AFTERUTILCONCURRENTLOCKRELEASEATTEMPT:
		case BEFOREINTRINSICLOCKACQUISITION:
		case BEFOREINTRINSICLOCKWAIT:
		case BEFOREUTILCONCURRENTLOCKACQUISITIONATTEMPT:
			useObject(attrs.getThreadId());
			useObject(attrs.getLockObjectId());
			break;
		case READWRITELOCK:
			final long id = attrs.getLong(AttributeType.ID);
			final long rLock = attrs.getLong(AttributeType.READ_LOCK_ID);
			final long wLock = attrs.getLong(AttributeType.WRITE_LOCK_ID);
			useObject(id);
			useObject(rLock);
			useObject(wLock);
			break;
		case FIELDDEFINITION:
		case OBJECTDEFINITION:
		case THREADDEFINITION:
			useObject(attrs.getLong(AttributeType.TYPE));
			break;
		default:
			break;
		}
	}

	@Override
	public void endDocument() throws SAXException {
		logState();
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
	 * Returns whether or not this object might be referenced.
	 * 
	 * @param id
	 * @return
	 */
	public boolean couldBeReferencedObject(final long id) {
		return f_referencedObjects.contains(id);
	}

	public boolean isSynthetic(final long field) {
		return f_synthetics.contains(field);
	}

	private void logState() {
		SLLogger.getLoggerFor(ScanRawFileFieldsPreScan.class).fine(
				"Range " + f_start + " to " + f_end
						+ "\n\tf_referencedObjects: "
						+ f_referencedObjects.size() + "\n\tf_usedFields "
						+ nestedSum1(f_usedFields) + "\n\tf_currentFields "
						+ nestedSum(f_currentFields));
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

}
