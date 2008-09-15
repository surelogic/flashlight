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
	private static final PhantomReference SHARED_BY_THREADS = Phantom.ofClass(Object.class);
	
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
	
	/**
	 * Mapping from objects to their fields to accelerate removal,
	 * when the object is garbage-collected
	 */
	private final Map<PhantomReference, Set<IKeyField>> f_objToFields =
		new HashMap<PhantomReference, Set<IKeyField>>();
	
	/**
	 * Mapping from fields to the thread it's used by (or SHARED_FIELD)
	 */
	private final Map<IKeyField,PhantomReference> f_objectToThread =
		new HashMap<IKeyField,PhantomReference>();		
		
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
	 * Remove info associated with fields of the given object, and return the
	 * set of thread-local fields
	 */
	private Set<IKeyField> removeFieldsForObject(final PhantomReference obj) {
		Set<IKeyField> fields  = f_objToFields.get(obj);
		if (fields == null) {
			return null;
		}
		Iterator<IKeyField> it = fields.iterator(); 
		while (it.hasNext()) {
			IKeyField f             = it.next();
			PhantomReference thread = f_objectToThread.remove(f);
			if (thread == SHARED_BY_THREADS) {
				it.remove(); 
			}
		}
		return fields;
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
			final PhantomReference lastThread = f_objectToThread.get(key);
			if (lastThread == null) {
				// First time to see this field: set to the current thread
				f_objectToThread.put(key, e.getWithinThread());
				addFieldToObject(key);
			}
			else if (lastThread == SHARED_BY_THREADS) {
				return;
			}
			else if (lastThread != e.getWithinThread()) {
				// Set field as shared
				f_objectToThread.put(key, SHARED_BY_THREADS);
			}		  
		}
		
		/**
		 * Mark the given field as associated with the given object
		 */
		private void addFieldToObject(final IKeyField field) {
			final PhantomReference obj = field.getWithin();
			Set<IKeyField> fields = f_objToFields.get(obj);
			if (fields == null) {
				fields = new HashSet<IKeyField>();
				f_objToFields.put(obj, fields);
			}
			fields.add(field);
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
			Set<IKeyField> deadFields = null;
			f_garbageCollectedObjectCount.addAndGet(f_deadList.size());
			for (IdPhantomReference pr : f_deadList) {
				if (pr.shouldBeIgnored()) {
					continue;
				}								
				deadFields = removeThreadLocalFieldsWithin(deadFields, pr);
				UnderConstruction.remove(pr);
				UtilConcurrent.remove(pr);
				f_eventCache.add(new GarbageCollectedObject(pr));
			}
			if (deadFields != null) {
				removeEventsAbout(deadFields);
			}
		}
	}

	/**
	 * Remove all mappings within the {@link #f_fieldToThread} map that are
	 * within the garbage collected object. 
	 * 
	 * @param pr
	 *            the phantom of the object.
	 */
	private Set<IKeyField> removeThreadLocalFieldsWithin(Set<IKeyField> deadFields, 
			final PhantomReference pr) {
		/*
		 * Collect up all the thread-local fields within the garbage collected
		 * object.
		 */
		final Set<IKeyField> threadLocalFields = removeFieldsForObject(pr);
		if (threadLocalFields != null) {
			if (deadFields == null) {
				deadFields = threadLocalFields;
			} else {
				deadFields.addAll(threadLocalFields);
			}
			markAsSingleThreaded(threadLocalFields);
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
	  Set<IKeyField> threadLocalFields = new HashSet<IKeyField>();
	  for (final Map.Entry<IKeyField, PhantomReference> entry : f_objectToThread.entrySet()) {
		  if (entry.getValue() != SHARED_BY_THREADS) {
			  threadLocalFields.add(entry.getKey());
		  }
	  }
	  removeEventsAbout(threadLocalFields);
	  markAsSingleThreaded(threadLocalFields);
	  f_objToFields.clear();	  
	  f_objectToThread.clear();
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
