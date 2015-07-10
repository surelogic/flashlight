package com.surelogic.flashlight.common.prep;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.carrotsearch.hppc.LongCollection;
import com.carrotsearch.hppc.LongLongMap;
import com.carrotsearch.hppc.LongLongScatterMap;
import com.carrotsearch.hppc.LongObjectMap;
import com.carrotsearch.hppc.LongObjectScatterMap;
import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.procedures.LongObjectProcedure;
import com.surelogic._flashlight.common.AttributeType;
import com.surelogic._flashlight.common.IdConstants;
import com.surelogic._flashlight.common.PreppedAttributes;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

public class ScanRawFileFieldsPreScan extends AbstractDataScan {

  private long f_elementCount = 0;
  private final long f_start;
  private final long f_end;

  private final LongSet f_referencedObjects = new LongScatterSet();
  private final LongSet f_indirectObjects = new LongScatterSet();

  /*
   * Collection of non-static fields accessed by multiple threads and the
   * objects that contain them, keyed by field
   */
  private final LongObjectMap<LongSet> f_usedFields = new LongObjectScatterMap<>();
  private final LongObjectMap<LongLongMap> f_currentFields = new LongObjectScatterMap<>();
  private final LongLongMap f_currentObjects = new LongLongScatterMap();
  private final LongSet f_synthetics;

  private void useObject(final long id) {
    if (f_start <= id && id <= f_end) {
      f_referencedObjects.add(id);
    }
  }

  private void indirectAccess(final long receiver, final long thread) {
    if (f_start <= receiver && receiver <= f_end) {
      final boolean oldEntryExists = f_currentObjects.containsKey(receiver);
      long lastPut = f_currentObjects.put(receiver, thread);
      if (oldEntryExists && lastPut != thread) {
        f_indirectObjects.add(receiver);
      }
    }
  }

  public ScanRawFileFieldsPreScan(final SLProgressMonitor monitor, final LongSet synthetics, final long start, final long end) {
    super(monitor);
    f_start = start;
    f_end = end;
    f_synthetics = synthetics;
  }

  /*
   * Field accesses
   */
  private void useField(final long field, final long thread, final long receiver) {
    if (f_start <= receiver && receiver <= f_end) {
      final LongLongMap objectFields = f_currentFields.get(receiver);
      if (objectFields != null) {
        final long _thread = objectFields.get(field);
        if (_thread == 0) {
          objectFields.put(field, thread);
        } else if (_thread != thread) {
          LongSet receivers = f_usedFields.get(field);
          if (receivers == null) {
            receivers = new LongScatterSet();
            f_usedFields.put(field, receivers);
          }
          receivers.add(receiver);
        }
      } else {
        final LongLongMap map = new LongLongScatterMap();
        map.put(field, thread);
        f_currentFields.put(receiver, map);
      }
    }
  }

  private void garbageCollect(final long objectId) {
    f_currentFields.remove(objectId);
    f_currentObjects.remove(objectId);
  }

  @SuppressWarnings("fallthrough")
  @Override
  public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
      throws SAXException {
    f_elementCount++;

    // modified to try and reduce computation overhead)
    if ((f_elementCount & 0x1f) == 0x1f) {
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
      indirectAccess(attrs.getLong(AttributeType.RECEIVER), attrs.getThreadId());
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
    case HAPPENSBEFORETHREAD:
      useObject(attrs.getLong(AttributeType.TOTHREAD));
      useObject(attrs.getLong(AttributeType.THREAD));
      break;
    case HAPPENSBEFOREOBJECT:
      useObject(attrs.getLong(AttributeType.OBJECT));
      useObject(attrs.getLong(AttributeType.THREAD));
      break;
    case HAPPENSBEFORECOLLECTION:
      useObject(attrs.getLong(AttributeType.OBJECT));
      useObject(attrs.getLong(AttributeType.THREAD));
      useObject(attrs.getLong(AttributeType.COLLECTION));
      break;
    case HAPPENSBEFOREEXEC:
      useObject(attrs.getLong(AttributeType.OBJECT));
      useObject(attrs.getLong(AttributeType.THREAD));
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
    final LongSet receivers = f_usedFields.get(field);
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
    return f_referencedObjects.contains(id) || f_indirectObjects.contains(id);
  }

  public boolean isIndirectlyAccessedObject(final long id) {
    return f_indirectObjects.contains(id);
  }

  public boolean isSynthetic(final long field) {
    return f_synthetics.contains(field);
  }

  private void logState() {
    SLLogger.getLoggerFor(ScanRawFileFieldsPreScan.class)
        .fine("Range " + f_start + " to " + f_end + "\n\tf_referencedObjects: " + f_referencedObjects.size() + "\n\tf_usedFields "
            + nestedSum1(f_usedFields) + "\n\tf_currentFields " + nestedSum(f_currentFields));
  }

  static class SumCollection implements LongObjectProcedure<LongCollection> {
    int result;

    public void apply(long key, LongCollection value) {
      result += value.size();
    }
  }

  private String nestedSum1(final LongObjectMap<LongSet> fields) {
    final SumCollection sum = new SumCollection();
    fields.forEach(sum);
    return Integer.toString(sum.result);
  }

  static class SumMap implements LongObjectProcedure<LongLongMap> {
    int result;

    public void apply(long key, LongLongMap value) {
      result += value.size();
    }
  }

  private String nestedSum(final LongObjectMap<LongLongMap> fields) {
    final SumMap sum = new SumMap();
    fields.forEach(sum);
    return Integer.toString(sum.result);
  }

}
