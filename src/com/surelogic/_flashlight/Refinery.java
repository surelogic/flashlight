package com.surelogic._flashlight;

import java.lang.ref.PhantomReference;
import java.util.*;
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
	private final BlockingQueue<List<Event>> f_rawQueue;

	private final BlockingQueue<List<Event>> f_outQueue;

	/**
	 * The desired size of {@link #f_eventCache}.
	 */
	private final int f_size;

	Refinery(final BlockingQueue<List<Event>> rawQueue,
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
		final boolean filter = true;		
		Store.flashlightThread();
		System.err.println("Filter events = "+filter);

		final List<List<Event>> buf = new ArrayList<List<Event>>();
		List<Event> last = null; // keep for reuse
		while (!f_finished) {
			try {
				f_rawQueue.drainTo(buf);
				for(List<Event> l : buf) {
					last = l;
					for(Event e : l) {
						if (e == FinalEvent.FINAL_EVENT) {
							/*
							 * We need to delay putting the final event on the out queue
							 * until all the thread-local events get added.
							 */
							f_finished = true;
							break;
						} else {
							f_eventCache.add(e);
							if (filter) {
								e.accept(f_detectSharedFieldsVisitor);
							}
						}
					}
				}
				buf.clear();
				if (f_finished) {
					for(Event e : Store.flushLocalQueues()) {
						f_eventCache.add(e);
						if (filter) {
							e.accept(f_detectSharedFieldsVisitor);
						}
					}
				}				
				
				processGarbageCollectedObjects(filter);
				if (f_finished) {
					removeRemainingThreadLocalFields();
				}
				
				final boolean xferd = transferEventsToOutQueue(null);
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
		last = new ArrayList<Event>();		
		last.add(new Time());
		last.add(FinalEvent.FINAL_EVENT);
		Store.putInQueue(f_outQueue, last);
		Store.log("refinery completed (" + f_garbageCollectedObjectCount.get()
				+ " object(s) garbage collected : "
				+ f_threadLocalFieldCount.get()
				+ " thread-local fields observed)");
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
			final IFieldInfo info   = e.getFieldInfo();
			final long key          = e.getFieldId();
			info.setLastThread(key, e.getWithinThread());	  
		}
	};

	/**
	 * Used to drain the garbage collected objects from {@link Phantom}. I hope
	 * that this is more efficient than having a local variable within
	 * {@link #processGarbageCollectedObjects()}.
	 */
	private final List<IdPhantomReference> f_deadList = new ArrayList<IdPhantomReference>();

	private final List<SingleThreadedField> f_singleThreadedList = new ArrayList<SingleThreadedField>();
	
	/**
	 * Examines each garbage collected object and cleans up our information
	 * about shared fields and thread-local fields.
	 * @param filter 
	 */
	private void processGarbageCollectedObjects(boolean filter) {
		f_deadList.clear();
		if (Phantom.drainTo(f_deadList) > 0) {
			Set<SingleThreadedField> deadFields = null;
			f_garbageCollectedObjectCount.addAndGet(f_deadList.size());
			for (IdPhantomReference pr : f_deadList) {
				if (pr.shouldBeIgnored()) {
					continue;
				}			
				if (filter) {
					deadFields = removeThreadLocalFieldsWithin(deadFields, pr);
				}
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
	private Set<SingleThreadedField> removeThreadLocalFieldsWithin(Set<SingleThreadedField> deadFields, 
			final PhantomReference pr) {
		/*
		 * Collect up all the thread-local fields within the garbage collected
		 * object.
		 */
		if (pr instanceof ObjectPhantomReference) {
			ObjectPhantomReference obj = (ObjectPhantomReference) pr;
			f_singleThreadedList.clear();
			obj.getFieldInfo().getSingleThreadedFields(f_singleThreadedList);
			
			if (!f_singleThreadedList.isEmpty()) {
				if (deadFields == null) {
					deadFields = new HashSet<SingleThreadedField>();
				}
				deadFields.addAll(f_singleThreadedList);
				markAsSingleThreaded(f_singleThreadedList);
				f_singleThreadedList.clear();
			}
		}	
		return deadFields;
	}
	
	private void markAsSingleThreaded(final Collection<SingleThreadedField> fields) {
		f_threadLocalFieldCount.addAndGet(fields.size());
		f_eventCache.addAll(fields);
	}

	/**
	 * Removes all <code>FieldAccess</code> events in the event cache about
	 * any field in the passed set.
	 * 
	 * @param fields
	 *            the set of fields to remove events about.
	 */
	private void removeEventsAbout(final Set<SingleThreadedField> fields) {
		for (Iterator<Event> j = f_eventCache.iterator(); j.hasNext();) {
			Event e = j.next();
			if (e instanceof FieldAccess) {
				if (fields.contains(e))
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
	  Set<SingleThreadedField> fields = ObjectPhantomReference.getAllSingleThreadedFields();
	  ObservedField.getFieldInfo().getSingleThreadedFields(fields);
	  removeEventsAbout(fields);
	  markAsSingleThreaded(fields);
	}

	/**
	 * Transfers events to the out queue. If we are finished then we add all
	 * events, otherwise we just add enough to keep our cache at {@link #f_size}.
	 * @param l 
	 */
	private boolean transferEventsToOutQueue(List<Event> l) {
		int transferCount = f_finished ? f_eventCache.size() : f_eventCache
				.size()
				- f_size;
		if (!f_finished && transferCount < 100) {
			return false;
		}
		final List<Event> buf;
		if (l != null) {
			l.clear();
			buf = l;			
		} else {
			buf = new ArrayList<Event>(transferCount);
		}
		while (transferCount > 0) {
			final Event e = f_eventCache.removeFirst();
			buf.add(e);
			transferCount--;
		}
		Store.putInQueue(f_outQueue, buf);
		return true;
	}
}
