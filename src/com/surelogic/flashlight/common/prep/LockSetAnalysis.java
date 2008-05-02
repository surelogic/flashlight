package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.DBQueryEmpty;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;

/**
 * The lock set analysis looks for bad field publishes during construction of an
 * object and for field access that hold an inconsistent set of locks.
 * 
 * @author nathan
 * 
 */
public class LockSetAnalysis extends DBQueryEmpty {

	private final long runId;

	public LockSetAnalysis(long runId) {
		this.runId = runId;
	}

	public void doPerform(final Query q) {
		q.prepared("LockSet.badPublishes").call(runId);
		q.prepared("LockSet.interestingFields").call(runId);
		q.prepared("LockSet.lockDurations", new ResultHandler<Void>() {
			public Void handle(final Result lockDurations) {
				final LockSets sets = new LockSets(lockDurations);
				q.prepared("LockSet.accesses", new ResultHandler<Void>() {
					public Void handle(final Result accesses) {
						for (final Row r : accesses) {
							// TS, InThread, Field, Receiver
							final Timestamp ts = r.nextTimestamp();
							final long thread = r.nextLong();
							final long field = r.nextLong();
							final Long receiver = r.nullableLong();
							final boolean read = "R".equals(r.nextString());
							if (receiver == null) {
								sets.staticAccess(ts, thread, field, read);
							} else {
								sets.instanceAccess(ts, thread, field,
										receiver, read);
							}
						}
						return null;
					}
				}).call(runId);
				sets.writeStatistics(q);
				return null;
			}
		}).call(runId);
	}

	private class LockSets {

		private final Map<Long, Set<Long>> fields;
		private final Map<Long, Map<Long, Set<Long>>> instances;
		private final Map<StaticInstance, Count> staticCounts;
		private final Map<FieldInstance, Count> counts;
		final ThreadLocks locks;

		public LockSets(Result lockDurations) {
			fields = new HashMap<Long, Set<Long>>();
			locks = new ThreadLocks(lockDurations);
			instances = new HashMap<Long, Map<Long, Set<Long>>>();
			staticCounts = new HashMap<StaticInstance, Count>();
			counts = new HashMap<FieldInstance, Count>();
		}

		public void writeStatistics(Query q) {
			final Queryable<Void> insertFieldLockSets = q
					.prepared("LockSet.insertFieldLockSets");
			final Queryable<Void> insertInstanceLockSets = q
					.prepared("LockSet.insertInstanceLockSets");
			for (final Entry<Long, Set<Long>> e : fields.entrySet()) {
				final long field = e.getKey();
				for (final long lock : e.getValue()) {
					insertFieldLockSets.call(runId, field, lock);
				}
			}
			for (final Entry<Long, Map<Long, Set<Long>>> e : instances
					.entrySet()) {
				final long field = e.getKey();
				Set<Long> fieldSet = null;
				for (final Entry<Long, Set<Long>> e1 : e.getValue().entrySet()) {
					final long receiver = e1.getKey();
					final Set<Long> instanceSet = e1.getValue();
					if (fieldSet == null) {
						fieldSet = new HashSet<Long>(instanceSet.size());
						fieldSet.addAll(instanceSet);
					} else {
						fieldSet.retainAll(instanceSet);
					}
					for (final long lock : instanceSet) {
						insertInstanceLockSets.call(runId, field, receiver,
								lock);
					}
				}
				for (final long lock : fieldSet) {
					insertFieldLockSets.call(runId, field, lock);
				}
			}
			final Queryable<Void> insertStaticCounts = q
					.prepared("LockSet.insertStaticCounts");
			final Queryable<Void> insertFieldCounts = q
					.prepared("LockSet.insertFieldCounts");
			for (final Entry<StaticInstance, Count> e : staticCounts.entrySet()) {
				final StaticInstance si = e.getKey();
				final Count c = e.getValue();
				insertStaticCounts.call(runId, si.thread, si.field, c.read,
						c.write);
			}
			for (final Entry<FieldInstance, Count> e : counts.entrySet()) {
				final FieldInstance fi = e.getKey();
				final Count c = e.getValue();
				insertFieldCounts.call(runId, fi.thread, fi.field, fi.receiver,
						c.read, c.write);
			}
		}

		public void staticAccess(Timestamp ts, long thread, long field,
				boolean read) {
			locks.ensureTime(ts);
			Set<Long> fieldSet = fields.get(field);
			final Collection<Long> lockSet = locks.getLocks(thread);
			if (fieldSet == null) {
				fieldSet = new HashSet<Long>(lockSet);
				fields.put(field, fieldSet);
			} else {
				fieldSet.retainAll(lockSet);
			}
			final StaticInstance si = new StaticInstance(thread, field);
			Count c = staticCounts.get(si);
			if (c == null) {
				c = new Count();
				staticCounts.put(si, c);
			}
			if (read) {
				c.read++;
			} else {
				c.write++;
			}
		}

		public void instanceAccess(Timestamp ts, long thread, long field,
				long receiver, boolean read) {
			locks.ensureTime(ts);
			Map<Long, Set<Long>> fieldMap = instances.get(field);
			if (fieldMap == null) {
				fieldMap = new HashMap<Long, Set<Long>>();
				instances.put(field, fieldMap);
			}
			Set<Long> instance = fieldMap.get(receiver);
			final Collection<Long> lockSet = locks.getLocks(thread);
			if (instance == null) {
				instance = new HashSet<Long>(lockSet);
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

		public StaticInstance(long thread, long field) {
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
		public boolean equals(Object obj) {
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

		public FieldInstance(long thread, long field, long receiver) {
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
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
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
	}

	/**
	 * Represents the locks held by a set of threads at a given point in time
	 * 
	 * @author nathan
	 * 
	 */
	private static class ThreadLocks {
		private final Iterator<Row> locks;
		private final Map<Long, Set<Long>> threads;
		private final NavigableSet<Lock> activeLocks;
		private Lock lock;

		ThreadLocks(Result lockDurations) {
			this.locks = lockDurations.iterator();
			this.activeLocks = new TreeSet<Lock>();
			this.threads = new HashMap<Long, Set<Long>>();
		}

		public Collection<Long> getLocks(long thread) {
			return getThreadSet(thread);
		}

		/**
		 * Bring the set of thread locks held up to date w/ the given time
		 * 
		 * @param time
		 */
		public void ensureTime(Timestamp time) {
			Lock oldLock;
			for (final Iterator<Lock> li = activeLocks.iterator(); li.hasNext()
					&& (oldLock = li.next()).end.before(time);) {
				getThreadSet(oldLock.thread).remove(oldLock.lock);
				li.remove();
			}
			// Check to see if we already have one, and if so, do we need to
			// get more
			if (lock != null) {
				if (lock.start.before(time)) {
					if (lock.end.after(time)) {
						activeLocks.add(lock);
						getThreadSet(lock.thread).add(lock.lock);
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
						getThreadSet(l.thread).add(l.lock);
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
		private Set<Long> getThreadSet(long id) {
			Set<Long> threadSet = threads.get(id);
			if (threadSet == null) {
				threadSet = new HashSet<Long>();
				threads.put(id, threadSet);
			}
			return threadSet;
		}

	}

	private static class Lock implements Comparable<Lock> {
		long thread;
		long lock;
		final Timestamp start;
		final Timestamp end;

		Lock(Timestamp endTime) {
			this.end = endTime;
			this.thread = 0;
			this.lock = 0;
			this.start = null;
		}

		Lock(Row row) {
			this.thread = row.nextLong();
			this.lock = row.nextLong();
			this.start = row.nextTimestamp();
			this.end = row.nextTimestamp();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((end == null) ? 0 : end.hashCode());
			result = prime * result + (int) (lock ^ (lock >>> 32));
			result = prime * result + ((start == null) ? 0 : start.hashCode());
			result = prime * result + (int) (thread ^ (thread >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Lock other = (Lock) obj;
			if (end == null) {
				if (other.end != null) {
					return false;
				}
			} else if (!end.equals(other.end)) {
				return false;
			}
			if (lock != other.lock) {
				return false;
			}
			if (start == null) {
				if (other.start != null) {
					return false;
				}
			} else if (!start.equals(other.start)) {
				return false;
			}
			if (thread != other.thread) {
				return false;
			}
			return true;
		}

		public int compareTo(Lock o) {
			return end.compareTo(o.end);
		}

	}

	public static void analyze(Connection conn, long runId) {
		new LockSetAnalysis(runId).perform(new ConnectionQuery(conn));
	}

}
