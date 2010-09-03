package com.surelogic.flashlight.common.prep;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.RowHandler;
import com.surelogic.common.jdbc.StringResultHandler;

public class SummaryInfo {

	private final List<Cycle> cycles;
	private final List<Lock> locks;
	private final List<Thread> threads;
	private final List<Field> emptyLockSetFields;
	private final String threadCount;
	private final String objectCount;
	private final String classCount;

	public SummaryInfo(final List<Cycle> cycles, final List<Lock> locks,
			final List<Thread> threads, final List<Field> emptyLockSetFields,
			final String threadCount, final String objectCount,
			final String classCount) {
		this.cycles = cycles;
		this.locks = locks;
		this.threads = threads;
		this.emptyLockSetFields = emptyLockSetFields;
		this.threadCount = threadCount;
		this.objectCount = objectCount;
		this.classCount = classCount;
	}

	public List<Cycle> getCycles() {
		return cycles;
	}

	public List<Lock> getLocks() {
		return locks;
	}

	public List<Thread> getThreads() {
		return threads;
	}

	public List<Field> getEmptyLockSetFields() {
		return emptyLockSetFields;
	}

	public String getThreadCount() {
		return threadCount;
	}

	public String getObjectCount() {
		return objectCount;
	}

	public String getClassCount() {
		return classCount;
	}

	public static class SummaryQuery implements DBQuery<SummaryInfo> {

		public SummaryInfo perform(final Query q) {
			List<Cycle> cycles = q.prepared("Deadlock.lockCycles",
					new DeadlockHandler()).call();
			List<Lock> locks = q.prepared("Deadlock.lockContention",
					new LockContentionHandler()).call();
			List<Thread> threads = q.prepared("SummaryInfo.threads",
					new ThreadContentionHandler()).call();
			List<Field> fields = new ArrayList<Field>();
			fields.addAll(q.prepared("SummaryInfo.emptyLockSets",
					new FieldHandler()).call());
			fields.addAll(q.prepared("SummaryInfo.emptyStaticLockSets",
					new FieldHandler()).call());
			Collections.sort(fields);
			String threadCount = q.prepared("SummaryInfo.threadCount",
					new StringResultHandler()).call();
			String classCount = q.prepared("SummaryInfo.classCount",
					new StringResultHandler()).call();
			String objectCount = q.prepared("SummaryInfo.objectCount",
					new StringResultHandler()).call();
			return new SummaryInfo(cycles, locks, threads, fields, threadCount,
					objectCount, classCount);
		}

	}

	public static class Field implements Comparable<Field> {
		private final String pakkage;
		private final String clazz;
		private final String name;
		private final String id;
		private final boolean isStatic;

		public Field(final String pakkage, final String clazz,
				final String name, final long id, final boolean isStatic) {
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.name = name;
			this.id = Long.toString(id);
			this.isStatic = isStatic;
		}

		public String getPackage() {
			return pakkage;
		}

		public String getClazz() {
			return clazz;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		public boolean isStatic() {
			return isStatic;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (clazz == null ? 0 : clazz.hashCode());
			result = prime * result + (isStatic ? 1231 : 1237);
			result = prime * result + (name == null ? 0 : name.hashCode());
			result = prime * result
					+ (pakkage == null ? 0 : pakkage.hashCode());
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
			if (getClass() != obj.getClass()) {
				return false;
			}
			Field other = (Field) obj;
			if (clazz == null) {
				if (other.clazz != null) {
					return false;
				}
			} else if (!clazz.equals(other.clazz)) {
				return false;
			}
			if (isStatic != other.isStatic) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (pakkage == null) {
				if (other.pakkage != null) {
					return false;
				}
			} else if (!pakkage.equals(other.pakkage)) {
				return false;
			}
			return true;
		}

		public int compareTo(final Field o) {
			int cmp = pakkage.compareTo(o.pakkage);
			if (cmp == 0) {
				cmp = clazz.compareTo(o.clazz);
				if (cmp == 0) {
					cmp = name.compareTo(o.name);
				}
			}
			return cmp;
		}

	}

	private static class FieldHandler implements RowHandler<Field> {

		public Field handle(final Row r) {
			return new Field(r.nextString(), r.nextString(), r.nextString(),
					r.nextLong(), r.nextBoolean());
		}

	}

	public static class Thread {
		private final String name;
		private final Date start;
		private final Date stop;
		private final long blockTime;

		public Thread(final String name, final Date start, final Date stop,
				final long blockTime) {
			this.name = name;
			this.start = start;
			this.stop = stop;
			this.blockTime = blockTime;
		}

		public String getName() {
			return name;
		}

		public long getBlockTime() {
			return blockTime;
		}

		public Date getStart() {
			return start;
		}

		public Date getStop() {
			return stop;
		}

	}

	private static class ThreadContentionHandler implements RowHandler<Thread> {

		public Thread handle(final Row r) {
			return new Thread(r.nextString(), r.nextTimestamp(),
					r.nextTimestamp(), r.nextLong());
		}

	}

	public static class Lock {
		private final String name;
		private final String id;
		private final int acquired;
		private final long blockTime;
		private final long averageBlock;

		public Lock(final String name, final int acquired,
				final long blockTime, final long averageBlock, final long id) {
			this.id = Long.toString(id);
			this.name = name;
			this.acquired = acquired;
			this.blockTime = blockTime;
			this.averageBlock = averageBlock;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		public int getAcquired() {
			return acquired;
		}

		public long getBlockTime() {
			return blockTime;
		}

		public long getAverageBlock() {
			return averageBlock;
		}

	}

	private static class LockContentionHandler implements RowHandler<Lock> {

		public Lock handle(final Row r) {
			return new Lock(r.nextString(), r.nextInt(), r.nextLong(),
					r.nextLong(), r.nextLong());
		}

	}

	public static class Cycle {
		private final Set<Edge> edges = new TreeSet<Edge>();
		private final int num;

		Cycle(final int num) {
			this.num = num;
		}

		public Set<Edge> getEdges() {
			return edges;
		}

		public int getNum() {
			return num;
		}

		Set<String> getLocks() {
			final Set<String> locks = new TreeSet<String>();
			for (final Edge e : edges) {
				locks.add(e.getHeld());
				locks.add(e.getAcquired());
			}
			return locks;
		}

	}

	public static class Edge implements Comparable<Edge> {
		private final String held;
		private final String heldId;
		private final String acquired;
		private final String acquiredId;
		private final int count;
		private final Timestamp first;
		private final Timestamp last;

		Edge(final String held, final String heldId, final String acquired,
				final String acquiredId, final int count,
				final Timestamp first, final Timestamp last) {
			this.held = held;
			this.heldId = heldId;
			this.acquired = acquired;
			this.acquiredId = acquiredId;
			this.count = count;
			this.first = first;
			this.last = last;
		}

		public String getHeld() {
			return held;
		}

		public String getAcquired() {
			return acquired;
		}

		public int getCount() {
			return count;
		}

		public Timestamp getFirst() {
			return first;
		}

		public Timestamp getLast() {
			return last;
		}

		public String getHeldId() {
			return heldId;
		}

		public String getAcquiredId() {
			return acquiredId;
		}

		public int compareTo(final Edge o) {
			int cmp = heldId.compareTo(o.heldId);
			if (cmp == 0) {
				cmp = acquiredId.compareTo(o.acquiredId);
			}
			return cmp;
		}

	}

	private static class DeadlockHandler implements ResultHandler<List<Cycle>> {

		public List<Cycle> handle(final Result result) {
			List<Cycle> cycles = new ArrayList<Cycle>();
			Cycle curCycle = new Cycle(-1);
			for (final Row r : result) {
				final int cycle = r.nextInt();
				if (curCycle.getNum() != cycle) {
					curCycle = new Cycle(cycle);
					cycles.add(curCycle);
				}
				final String held = r.nextString();
				final String heldId = r.nextString();
				final String acquired = r.nextString();
				final String acquiredId = r.nextString();
				final int count = r.nextInt();
				final Timestamp first = r.nextTimestamp();
				final Timestamp last = r.nextTimestamp();
				curCycle.getEdges().add(
						new Edge(held, heldId, acquired, acquiredId, count,
								first, last));
			}

			return cycles;
		}

	}
}
