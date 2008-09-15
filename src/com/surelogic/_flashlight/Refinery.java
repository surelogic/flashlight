package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Takes events from the raw queue, refines them, and then places them on the
 * out queue.
 * <P>
 * The refinery tries to identify fields that are accessed only from a single
 * thread during their entire lifetime. If such a field can be identified then
 * events about it are not placed on the out queue. Removal of events is a best
 * effort and some events about the field may be output. These events will have
 * to be removed during data prep.
 */
final class Refinery extends Thread {

	private final BlockingQueue<Event> f_rawQueue;

	private final BlockingQueue<List<Event>> f_outQueue;

	/**
	 * The desired size of {@link #f_eventCache}.
	 */
	private final int f_size;

	Refinery(final BlockingQueue<Event> rawQueue,
			final BlockingQueue<List<Event>> outQueue, final int size) {
		super("flashlight-refinery");
		assert rawQueue != null;
		f_rawQueue = rawQueue;
		assert outQueue != null;
		f_outQueue = outQueue;
		f_size = size;
	}

	private boolean f_finished = false;

	private final LinkedList<Event> f_eventCache = new LinkedList<Event>();

	private final AtomicLong f_garbageCollectedObjectCount = new AtomicLong();

	private final AtomicLong f_threadLocalFieldCount = new AtomicLong();

	@Override
	public void run() {
		Store.flashlightThread();

		final List<Event> buf = new ArrayList<Event>();
		while (!f_finished) {
			try {
				f_rawQueue.drainTo(buf);
				for(Event e : buf) {
					if (e == FinalEvent.FINAL_EVENT) {
						/*
						 * We need to delay putting the final event on the out queue
						 * until all the thread-local events get added.
						 */
						f_finished = true;
						break;
					} else {
						f_eventCache.add(e);
						e.accept(f_detectSharedFieldsVisitor);
					}
				}
				buf.clear();
				processGarbageCollectedObjects();
				if (f_finished) {
					removeRemainingThreadLocalFields();
				}
				
				final boolean xferd = transferEventsToOutQueue();
				if (!xferd) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// Ignored
					}
				}
			} catch (IllegalArgumentException e) {
				Store.logAProblem("refinery was interrupted...a bug");
			}
		}
		buf.clear();
		buf.add(new Time());
		buf.add(FinalEvent.FINAL_EVENT);
		Store.putInQueue(f_outQueue, buf);
		Store.log("refinery completed (" + f_garbageCollectedObjectCount.get()
				+ " object(s) garbage collected : "
				+ f_threadLocalFieldCount.get()
				+ " thread-local fields observed)");
	}

	
	/**
	 * A double layer map whose primary goal is to map field keys to threads, but
	 * we also need to make it very cheap to remove all the fields that belong
	 * to a particular object.  So the first layer of the map is from object to
	 * (field to thread).  This still keeps the primary field to thread operations
	 * cheap because each field knows the object it belongs to.  But it makes
	 * removing all the fields for an object much cheaper than before because we
	 * don't have to iterate over all the keys anymore.
	 */
	private final Map<PhantomReference, Map<IKeyField, PhantomReference>> f_objToFieldToThread =
	  new HashMap<PhantomReference, Map<IKeyField, PhantomReference>>();
	
	
	/**
	 * Check that that the given field is mapped to the given thread.  If the
	 * field is not currently mapped to anything, then the field is mapped to
	 * the given thread, and the method returns <code>true</code>.  If the field
	 * is mapped to a thread, then this method returns whether that thread is
	 * the same as the given thread.
	 */ 
	private boolean testFieldToThread(
	    final IKeyField field, final PhantomReference thread) {
	  final PhantomReference obj = field.getWithin();
	  Map<IKeyField, PhantomReference> fieldToThread = f_objToFieldToThread.get(obj);
	  if (fieldToThread == null) {
	    fieldToThread = new HashMap<IKeyField, PhantomReference>();
      f_objToFieldToThread.put(obj, fieldToThread);
	    fieldToThread.put(field, thread);
	    return true;
	  } else {
	    return fieldToThread.get(field) == thread;
	  }
	}

	/**
	 * Remove the field to thread mapping for the given field.
	 */
	private void removeFieldToThread(final IKeyField field) {
    final PhantomReference obj = field.getWithin();
    Map<IKeyField, PhantomReference> fieldToThread = f_objToFieldToThread.get(obj);
    if (fieldToThread != null) {
      fieldToThread.remove(field);
    }
	}
	
	/**
	 * Get the field to thread mapping for a given object and remove it from the
	 * global table.
	 */
	private Map<IKeyField, PhantomReference> removeFieldsForObject(
	    final PhantomReference obj) {
	  return f_objToFieldToThread.remove(obj);
	}
	    
	
	/**
   * A map whose primary goal is to keep a set of field keys that have been
   * observed to be shared until the enclosing object is garbage collected. We
   * need to be able to cheaply add fields to the set, but to also cheaply
   * remove all the fields that belong to particular object. So from objects to
   * sets of fields. This still keeps the primary field operations cheap because
   * each field knows the object it belongs to. But it makes removing all the
   * fields for an object much cheaper than before because we don't have to
   * iterate over all the fields anymore.
   */
  private final Map<PhantomReference, Set<IKeyField>> f_objToSharedFields =
    new HashMap<PhantomReference, Set<IKeyField>>();

  /**
   * Is the given field shared?
   */
  private boolean isSharedField(final IKeyField field) {
    final Set<IKeyField> sharedFields = f_objToSharedFields.get(field.getWithin());
    return (sharedFields == null) ? false : sharedFields.contains(field);
  }

  /**
   * Mark the given field as shared 
   */
  private void addSharedField(final IKeyField field) {
    final PhantomReference obj = field.getWithin();
    Set<IKeyField> sharedFields = f_objToSharedFields.get(obj);
    if (sharedFields == null) {
      sharedFields = new HashSet<IKeyField>();
      f_objToSharedFields.put(obj, sharedFields);
    }
    sharedFields.add(field);
  }
  
	private final EventVisitor f_detectSharedFieldsVisitor = new EventVisitor() {

		@Override
		void visit(final FieldReadInstance e) {
			visitFieldAccess(e);
		}

		@Override
		void visit(final FieldWriteInstance e) {
			visitFieldAccess(e);
		}

		@Override
		void visit(final FieldReadStatic e) {
			visitFieldAccess(e);
		}

		@Override
		void visit(final FieldWriteStatic e) {
			visitFieldAccess(e);
		}

		private void visitFieldAccess(final FieldAccess e) {
			final IKeyField key = e.getKey();
			if (isSharedField(key)) 
				return;
			if (!testFieldToThread(key, e.getWithinThread())) {
        /*
         * Shared access observed on this field.
         */
			  removeFieldToThread(key);
			  addSharedField(key);
			}			  
		}
	};

	/**
	 * Used to drain the garbage collected objects from {@link Phantom}. I hope
	 * that this is more efficient than having a local variable within
	 * {@link #processGarbageCollectedObjects()}.
	 */
	private final List<IdPhantomReference> f_deadList = new ArrayList<IdPhantomReference>();

	/**
	 * Examines each garbage collected object and cleans up our information
	 * about shared fields and thread-local fields.
	 */
	private void processGarbageCollectedObjects() {
		f_deadList.clear();
		if (Phantom.drainTo(f_deadList) > 0) {
			Map<IKeyField, PhantomReference> deadFields = null;
			f_garbageCollectedObjectCount.addAndGet(f_deadList.size());
			for (IdPhantomReference pr : f_deadList) {
				if (pr.shouldBeIgnored()) {
					continue;
				}				
				removeSharedFieldsWithin(pr);
				
				deadFields = removeThreadLocalFieldsWithin(deadFields, pr);
				UnderConstruction.remove(pr);
				UtilConcurrent.remove(pr);
				f_eventCache.add(new GarbageCollectedObject(pr));
			}
			if (deadFields != null) {
				removeEventsAbout(deadFields.keySet());
			}
		}
	}

	/**
	 * Removes all fields within the {@link #f_sharedFields} list that are
	 * within the garbage collected object. This is reasonable because we will
	 * not see any more access to this object.
	 * 
	 * @param pr
	 *            the phantom of the object.
	 */
	private void removeSharedFieldsWithin(final PhantomReference pr) {
	  f_objToSharedFields.remove(pr);
	}

	/**
	 * Remove all mappings within the {@link #f_fieldToThread} map that are
	 * within the garbage collected object. 
	 * 
	 * @param pr
	 *            the phantom of the object.
	 */
	private Map<IKeyField,PhantomReference> removeThreadLocalFieldsWithin(Map<IKeyField,PhantomReference> deadFields, 
			final PhantomReference pr) {
		/*
		 * Collect up all the thread-local fields within the garbage collected
		 * object.
		 */
		final Map<IKeyField, PhantomReference> fieldMap = removeFieldsForObject(pr);
		if (fieldMap != null) {
			if (deadFields == null) {
				deadFields = fieldMap;
			} else {
				deadFields.putAll(fieldMap);
			}
			markAsSingleThreaded(fieldMap.keySet());
		}
		return deadFields;
	}

	private void markAsSingleThreaded(final Set<IKeyField> fields) {
		f_threadLocalFieldCount.addAndGet(fields.size());
		for (IKeyField field : fields) {
			f_eventCache.add(field.getSingleThreadedEventAbout());
		}
	}

	/**
	 * Removes all <code>FieldAccess</code> events in the event cache about
	 * any field in the passed set.
	 * 
	 * @param fields
	 *            the set of fields to remove events about.
	 */
	private void removeEventsAbout(final Set<IKeyField> fields) {
		for (Iterator<Event> j = f_eventCache.iterator(); j.hasNext();) {
			Event e = j.next();
			if (e instanceof FieldAccess) {
				FieldAccess fa = (FieldAccess) e;
				IKeyField field = fa.getKey();
				if (fields.contains(field))
					j.remove();
			}
		}
	}

	/**
	 * When the refinery finishes up any remaining fields that have been
	 * observed to be thread-local can have events about them removed. This
	 * method also cleans up the collections used to determine if a field is
	 * thread-local or shared.
	 */
	private void removeRemainingThreadLocalFields() {
	  Set<IKeyField> fields = new HashSet<IKeyField>();
	  for (final Map.Entry<PhantomReference, Map<IKeyField, PhantomReference>> entry : f_objToFieldToThread.entrySet()) {
	    fields.addAll(entry.getValue().keySet());
	  }
	  removeEventsAbout(fields);
	  markAsSingleThreaded(fields);
	  f_objToFieldToThread.clear();	  
	  f_objToSharedFields.clear();
	}

	/**
	 * Transfers events to the out queue. If we are finished then we add all
	 * events, otherwise we just add enough to keep our cache at {@link #f_size}.
	 */
	private boolean transferEventsToOutQueue() {
		int transferCount = f_finished ? f_eventCache.size() : f_eventCache
				.size()
				- f_size;
		if (!f_finished && transferCount < 100) {
			return false;
		}
		final List<Event> buf = new ArrayList<Event>(transferCount);
		while (transferCount > 0) {
			final Event e = f_eventCache.removeFirst();
			buf.add(e);
			transferCount--;
		}
		Store.putInQueue(f_outQueue, buf);
		return true;
	}
}
