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

	private final BlockingQueue<Event> f_outQueue;

	/**
	 * The desired size of {@link #f_eventCache}.
	 */
	private final int f_size;

	Refinery(final BlockingQueue<Event> rawQueue,
			final BlockingQueue<Event> outQueue, final int size) {
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

		while (!f_finished) {
			try {
				Event e = f_rawQueue.take();
				if (e == FinalEvent.FINAL_EVENT)
					/*
					 * We need to delay putting the final event on the out queue
					 * until all the thread-local events get added.
					 */
					f_finished = true;
				else {
					f_eventCache.add(e);
					e.accept(f_detectSharedFieldsVisitor);
				}
				processGarbageCollectedObjects();
				if (f_finished) {
					removeRemainingThreadLocalFields();
				}
				transferEventsToOutQueue();
			} catch (InterruptedException e) {
				Store.logAProblem("refinery was interrupted...a bug");
			}
		}
		Store.putInQueue(f_outQueue, FinalEvent.FINAL_EVENT);
		Store.log("refinery completed (" + f_garbageCollectedObjectCount.get()
				+ " object(s) garbage collected : "
				+ f_threadLocalFieldCount.get()
				+ " thread-local fields observed)");
	}

	/**
	 * A map from a field key to the phantom reference to the only thread
	 * observed reading or writing the field. This map only contains fields as
	 * keys that, so far, are thread-local. If a field key has been observed to
	 * be shared it will not be in this map, it will be in
	 * {@link #f_sharedFields}.
	 */
	private final Map<KeyField, PhantomReference> f_fieldToThread = new HashMap<KeyField, PhantomReference>();

	/**
	 * Maintains a list of field keys that have been observed to be shared until
	 * the enclosing object is garbage collected.
	 */
	private final List<KeyField> f_sharedFields = new LinkedList<KeyField>();

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
			final KeyField key = e.getKey();
			if (f_sharedFields.contains(key))
				return;
			PhantomReference thread = f_fieldToThread.get(key);
			if (thread == null) {
				/*
				 * First time we have see an access to this field, assume access
				 * will be single-threaded.
				 */
				f_fieldToThread.put(key, e.getWithinThread());
			} else {
				if (thread != e.getWithinThread()) {
					/*
					 * Shared access observed on this field.
					 */
					f_fieldToThread.remove(key);
					f_sharedFields.add(key);
				}
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
			f_garbageCollectedObjectCount.addAndGet(f_deadList.size());
			for (IdPhantomReference pr : f_deadList) {
				removeSharedFieldsWithin(pr);
				removeThreadLocalFieldsWithin(pr);
				UnderConstruction.remove(pr);
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
		for (Iterator<KeyField> i = f_sharedFields.iterator(); i.hasNext();) {
			KeyField field = i.next();
			if (field.isWithin(pr))
				i.remove();
		}
	}

	/**
	 * Remove all mappings within the {@link #f_fieldToThread} map that are
	 * within the garbage collected object. All <code>FieldAccess</code>
	 * events about these fields are removed from {@link #f_eventCache}.
	 * 
	 * @param pr
	 *            the phantom of the object.
	 */
	private void removeThreadLocalFieldsWithin(final PhantomReference pr) {
		/*
		 * Collect up all the thread-local fields within the garbage collected
		 * object.
		 */
		final Set<KeyField> threadLocalFields = new HashSet<KeyField>();
		for (Iterator<KeyField> i = f_fieldToThread.keySet().iterator(); i
				.hasNext();) {
			KeyField field = i.next();
			if (field.isWithin(pr)) {
				i.remove();
				threadLocalFields.add(field);
			}
		}
		if (!threadLocalFields.isEmpty())
			removeEventsAbout(threadLocalFields);
	}

	/**
	 * Removes all <code>FieldAccess</code> events in the event cache about
	 * any field in the passed set.
	 * 
	 * @param fields
	 *            the set of fields to remove events about.
	 */
	private void removeEventsAbout(final Set<KeyField> fields) {
		for (Iterator<Event> j = f_eventCache.iterator(); j.hasNext();) {
			Event e = j.next();
			if (e instanceof FieldAccess) {
				FieldAccess fa = (FieldAccess) e;
				KeyField field = fa.getKey();
				if (fields.contains(field))
					j.remove();
			}
		}
		f_threadLocalFieldCount.addAndGet(fields.size());
		for (KeyField field : fields) {
			f_eventCache.add(field.getSingleThreadedEventAbout());
		}
	}

	/**
	 * When the refinery finishes up any remaining fields that have been
	 * observed to be thread-local can have events about them removed. This
	 * method also cleans up the collections used to determine if a field is
	 * thread-local or shared.
	 */
	private void removeRemainingThreadLocalFields() {
		if (!f_fieldToThread.keySet().isEmpty())
			removeEventsAbout(f_fieldToThread.keySet());
		f_fieldToThread.clear();
		f_sharedFields.clear();
	}

	/**
	 * Transfers events to the out queue. If we are finished then we add all
	 * events, otherwise we just add enough to keep our cache at {@link #f_size}.
	 */
	private void transferEventsToOutQueue() {
		int transferCount = f_finished ? f_eventCache.size() : f_eventCache
				.size()
				- f_size;
		while (transferCount > 0) {
			final Event e = f_eventCache.removeFirst();
			Store.putInQueue(f_outQueue, e);
			transferCount--;
		}
	}
}
