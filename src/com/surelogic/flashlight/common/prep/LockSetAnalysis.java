package com.surelogic.flashlight.common.prep;

import gnu.trove.TLongHashSet;
import gnu.trove.TLongIterator;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.NullResultHandler;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Nulls;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;

/**
 * The lock set analysis looks for bad field publishes during construction of an
 * object and for field access that hold an inconsistent set of locks.
 */
public class LockSetAnalysis implements IPostPrep {

	private final Logger log = SLLogger.getLoggerFor(LockSetAnalysis.class);

	private Connection c;

	public String getDescription() {
		return "Performing lock set analysis";
	}

	private void commit() {
		try {
			c.commit();
		} catch (final SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public void doPostPrep(final Connection c, final SLProgressMonitor mon) {
		this.c = c;
		doPerform(new ConnectionQuery(c), mon);
	}

	public void doPerform(final Query q, final SLProgressMonitor mon) {
		log.fine("Inserting bad publishes.");
		q.prepared("LockSet.v2.badPublishes", new NullRowHandler() {
			Queryable<Void> insert = q.prepared("LockSet.v2.insertBadPublish");

			@Override
			protected void doHandle(final Row r) {
				insert.call(r.nextLong(), r.nextLong());
			}
		}).call();
		commit();
		if (mon.isCanceled()) {
			return;
		}
		log.fine("Inserting interesting fields.");
		q.prepared("LockSet.v2.interestingFields", new NullRowHandler() {
			Queryable<Void> insert = q
					.prepared("LockSet.v2.insertInterestingField");

			@Override
			protected void doHandle(final Row r) {
				insert.call(r.nextLong(), Nulls.coerce(r.nullableLong()));
			}
		}).call();
		commit();
		if (mon.isCanceled()) {
			return;
		}
		log.fine("Scanning lock durations and accesses.");
		q.prepared("LockSet.v2.lockDurations", new NullResultHandler() {
			@Override
			public void doHandle(final Result lockDurations) {
				final LockSets sets = new LockSets(lockDurations, q
						.prepared("LockSet.v2.updateAccessLocksHeld"));
				q.prepared("LockSet.v2.accesses", new NullResultHandler() {
					@Override
					public void doHandle(final Result accesses) {
						int count = 0;
						for (final Row r : accesses) {
							if (++count % 10000 == 0) {
								if (mon.isCanceled()) {
									return;
								}
							}
							// TS, InThread, Field, Receiver
							final long id = r.nextLong();
							final Timestamp ts = r.nextTimestamp();
							final long thread = r.nextLong();
							final long field = r.nextLong();
							final Long receiver = r.nullableLong();
							final boolean read = "R".equals(r.nextString());
							final boolean underConstruction = "Y".equals(r
									.nextString());
							if (underConstruction) {
								sets.instanceUnderConstruction(id, ts, thread,
										field, receiver, read);
							} else if (receiver == null) {
								sets.staticAccess(id, ts, thread, field, read);
							} else {
								sets.instanceAccess(id, ts, thread, field,
										receiver, read);
							}
						}
					}
				}).call();
				if (mon.isCanceled()) {
					return;
				}
				log.fine("Writing out locking statistics.");
				sets.writeStatistics(q);
				// Add foreign key to ACCESSLOCKSHELD table
				commit();
				log.fine("Adding locking statistic constraints");
				q.prepared("LockSet.v2.accessLocksHeldConstraint").call();
				q.prepared("LockSet.v2.accessLockAcquisitionConstraint").call();
			}
		}).call();
	}

	private class LockSets {

		private final TLongObjectHashMap<TLongHashSet> fields;
		private final TLongObjectHashMap<TLongObjectHashMap<TLongHashSet>> instances;
		private final Map<StaticInstance, StaticCount> staticCounts;
		private final Map<FieldInstance, Count> counts;
		final ThreadLocks locks;
		final Queryable<?> updateAccess;

		public LockSets(final Result lockDurations,
				final Queryable<?> updateAccess) {
			fields = new TLongObjectHashMap<TLongHashSet>();
			locks = new ThreadLocks(lockDurations);
			instances = new TLongObjectHashMap<TLongObjectHashMap<TLongHashSet>>();
			staticCounts = new HashMap<StaticInstance, StaticCount>();
			counts = new HashMap<FieldInstance, Count>();
			this.updateAccess = updateAccess;
		}

		public void writeStatistics(final Query q) {
			final Queryable<Void> insertFieldLockSets = q
					.prepared("LockSet.v2.insertFieldLockSets");
			final Queryable<Void> insertInstanceLockSets = q
					.prepared("LockSet.v2.insertInstanceLockSets");
			log.fine("Static field lock sets.");
			fields.forEachEntry(new TLongObjectProcedure<TLongHashSet>() {
				int count = 0;

				public boolean execute(final long field,
						final TLongHashSet locks) {
					for (final TLongIterator it = locks.iterator(); it
							.hasNext();) {
						insertFieldLockSets.call(field, it.next());
						if (++count % 10000 == 0) {
							commit();
						}
					}
					return true;
				}
			});
			log.fine("Instance lock sets.");
			instances
					.forEachEntry(new TLongObjectProcedure<TLongObjectHashMap<TLongHashSet>>() {
						int count;

						public boolean execute(final long field,
								final TLongObjectHashMap<TLongHashSet> instance) {
							final TLongHashSet fieldSet = new TLongHashSet();
							instance
									.forEachEntry(new TLongObjectProcedure<TLongHashSet>() {
										boolean first = true;

										public boolean execute(
												final long receiver,
												final TLongHashSet instanceSet) {
											if (first) {
												fieldSet.addAll(instanceSet
														.toArray());
												first = false;
											} else {
												fieldSet.retainAll(instanceSet
														.toArray());
											}
											for (final TLongIterator it = instanceSet
													.iterator(); it.hasNext();) {
												insertInstanceLockSets.call(
														field, receiver, it
																.next());
											}
											if (++count % 10000 == 0) {
												commit();
											}
											return true;
										}
									});
							for (final TLongIterator it = fieldSet.iterator(); it
									.hasNext();) {
								insertFieldLockSets.call(field, it.next());
								if (++count % 10000 == 0) {
									commit();
								}
							}
							return true;
						}
					});

			final Queryable<Void> insertStaticCounts = q
					.prepared("LockSet.v2.insertStaticCounts");
			final Queryable<Void> insertFieldCounts = q
					.prepared("LockSet.v2.insertFieldCounts");
			int count = 0;
			log.fine("Locking counts.");
			for (final Entry<StaticInstance, StaticCount> e : staticCounts
					.entrySet()) {
				final StaticInstance si = e.getKey();
				final StaticCount c = e.getValue();
				insertStaticCounts.call(si.thread, si.field, c.read, c.write);
				if (++count % 10000 == 0) {
					commit();
				}
			}
			for (final Entry<FieldInstance, Count> e : counts.entrySet()) {
				final FieldInstance fi = e.getKey();
				final Count c = e.getValue();
				insertFieldCounts.call(fi.thread, fi.field, fi.receiver,
						c.read, c.write, c.readUC, c.writeUC);
				if (++count % 10000 == 0) {
					commit();
				}
			}
		}

		public void staticAccess(final long id, final Timestamp ts,
				final long thread, final long field, final boolean read) {
			locks.ensureTime(ts);
			TLongHashSet fieldSet = fields.get(field);
			final long[] lockSet = locks.getLocks(thread);
			updateAccess.call(id, lockSet.length, Nulls.coerce(locks
					.getLastAcquisition(thread)));
			if (fieldSet == null) {
				fieldSet = new TLongHashSet(lockSet.length);
				fieldSet.addAll(lockSet);
				fields.put(field, fieldSet);
			} else {
				fieldSet.retainAll(lockSet);
			}
			final StaticInstance si = new StaticInstance(thread, field);
			StaticCount c = staticCounts.get(si);
			if (c == null) {
				c = new StaticCount();
				staticCounts.put(si, c);
			}
			if (read) {
				c.read++;
			} else {
				c.write++;
			}
		}

		public void instanceUnderConstruction(final long id,
				final Timestamp ts, final long thread, final long field,
				final Long receiver, final boolean read) {
			locks.ensureTime(ts);
			updateAccess.call(id, locks.getLocks(thread).length, Nulls
					.coerce(locks.getLastAcquisition(thread)));
			final FieldInstance fi = new FieldInstance(thread, field, receiver);
			Count count = counts.get(fi);
			if (count == null) {
				count = new Count();
				counts.put(fi, count);
			}
			if (read) {
				count.readUC++;
			} else {
				count.writeUC++;
			}
		}

		public void instanceAccess(final long id, final Timestamp ts,
				final long thread, final long field, final long receiver,
				final boolean read) {
			locks.ensureTime(ts);
			TLongObjectHashMap<TLongHashSet> fieldMap = instances.get(field);
			if (fieldMap == null) {
				fieldMap = new TLongObjectHashMap<TLongHashSet>();
				instances.put(field, fieldMap);
			}
			TLongHashSet instance = fieldMap.get(receiver);
			final long[] lockSet = locks.getLocks(thread);
			updateAccess.call(id, lockSet.length, Nulls.coerce(locks
					.getLastAcquisition(thread)));
			if (instance == null) {
				instance = new TLongHashSet();
				instance.addAll(lockSet);
				fieldMap.put(receiver, instance);
			} else {
				instance.retainAll(lockSet);
			}
			final FieldInstance fi = new FieldInstance(thread, field, receiver);
			Count count = counts.get(fi);
			if (count == null) {
				count = new Count();
				counts.put(fi, count);
			}
			if (read) {
				count.read++;
			} else {
				count.write++;
			}
		}
	}

	private static class StaticInstance {
		long thread;
		long field;

		public StaticInstance(final long thread, final long field) {
			super();
			this.thread = thread;
			this.field = field;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (field ^ (field >>> 32));
			result = prime * result + (int) (thread ^ (thread >>> 32));
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			final StaticInstance other = (StaticInstance) obj;
			if (field != other.field) {
				return false;
			}
			if (thread != other.thread) {
				return false;
			}
			return true;
		}

	}

	private static class FieldInstance {
		long thread;
		long field;
		long receiver;

		public FieldInstance(final long thread, final long field,
				final long receiver) {
			super();
			this.thread = thread;
			this.field = field;
			this.receiver = receiver;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (field ^ (field >>> 32));
			result = prime * result + (int) (receiver ^ (receiver >>> 32));
			result = prime * result + (int) (thread ^ (thread >>> 32));
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			final FieldInstance other = (FieldInstance) obj;
			if (field != other.field) {
				return false;
			}
			if (receiver != other.receiver) {
				return false;
			}
			if (thread != other.thread) {
				return false;
			}
			return true;
		}

	}

	private static class Count {
		long read;
		long write;
		long readUC;
		long writeUC;
	}

	private static class StaticCount {
		long read;
		long write;
	}

	/**
	 * Represents the locks held by a set of threads at a given point in time
	 * 
	 * @author nathan
	 * 
	 */
	private static class ThreadLocks {
		private final Iterator<Row> locks;
		private final TLongObjectHashMap<TreeSet<Lock>> threads;
		private final TreeSet<Lock> activeLocks;
		private Lock lock;

		ThreadLocks(final Result lockDurations) {
			this.locks = lockDurations.iterator();
			this.activeLocks = new TreeSet<Lock>(new Comparator<Lock>() {
				public int compare(final Lock o1, final Lock o2) {
					return o1.end.compareTo(o2.end);
				}
			});
			this.threads = new TLongObjectHashMap<TreeSet<Lock>>();
		}

		/**
		 * Get the collection of locks a thread currently holds.
		 * 
		 * @param thread
		 * @return
		 */
		public long[] getLocks(final long thread) {
			final Collection<Lock> set = getThreadSet(thread);
			final long[] locks = new long[set.size()];
			int i = 0;
			for (final Lock l : set) {
				locks[i++] = l.lock;
			}
			return locks;
		}

		/**
		 * Get the lock event id of the last lock acquisition made in this
		 * thread before the given time.
		 * 
		 * @param thread
		 * @return
		 */
		public Long getLastAcquisition(final long thread) {
			final TreeSet<Lock> set = getThreadSet(thread);
			return set.size() == 0 ? null : set.last().startEvent;
		}

		/**
		 * Bring the set of thread locks held up to date w/ the given time
		 * 
		 * @param time
		 */
		public void ensureTime(final Timestamp time) {
			Lock oldLock;
			for (final Iterator<Lock> li = activeLocks.iterator(); li.hasNext()
					&& (oldLock = li.next()).end.before(time);) {
				getThreadSet(oldLock.thread).remove(oldLock);
				li.remove();
			}
			// Check to see if we already have one, and if so, do we need to
			// get more
			if (lock != null) {
				if (lock.start.before(time)) {
					if (lock.end.after(time)) {
						activeLocks.add(lock);
						getThreadSet(lock.thread).add(lock);
					}
				} else {
					return;
				}
			}
			// Add all of the locks that start before the given time and end
			// after the given time
			while (locks.hasNext()) {
				final Lock l = new Lock(locks.next());
				if (l.start.before(time)) {
					if (l.end.after(time)) {
						activeLocks.add(l);
						getThreadSet(l.thread).add(l);
					}
				} else {
					lock = l;
					return;
				}
			}
		}

		/**
		 * Get the set of locks a thread currently holds.
		 * 
		 * @param id
		 * @return
		 */
		private TreeSet<Lock> getThreadSet(final long id) {
			TreeSet<Lock> threadSet = threads.get(id);
			if (threadSet == null) {
				threadSet = new TreeSet<Lock>(new Comparator<Lock>() {
					public int compare(final Lock o1, final Lock o2) {
						return o1.start.compareTo(o2.start);
					}
				});
				threads.put(id, threadSet);
			}
			return threadSet;
		}

	}

	private static class Lock {
		final long thread;
		final long lock;
		final long startEvent;
		final Timestamp start;
		final Timestamp end;

		Lock(final Timestamp endTime) {
			this.end = endTime;
			this.thread = 0;
			this.lock = 0;
			this.start = null;
			this.startEvent = 0;
		}

		Lock(final Row row) {
			this.thread = row.nextLong();
			this.lock = row.nextLong();
			this.start = row.nextTimestamp();
			this.end = row.nextTimestamp();
			this.startEvent = row.nextLong();
		}

	}
}
