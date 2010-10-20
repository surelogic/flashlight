package com.surelogic.flashlight.common.prep;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.RowHandler;
import com.surelogic.common.jdbc.StringResultHandler;
import com.surelogic.common.jdbc.StringRowHandler;
import com.surelogic.flashlight.schema.Trace;

public class SummaryInfo {

	private final List<Cycle> cycles;
	private final List<Lock> locks;
	private final List<Thread> threads;
	private final List<Field> emptyLockSetFields;
	private final String threadCount;
	private final String objectCount;
	private final String classCount;
	private final Site root;

	public SummaryInfo(final List<Cycle> cycles, final List<Lock> locks,
			final List<Thread> threads, final List<Field> emptyLockSetFields,
			final String threadCount, final String objectCount,
			final String classCount, final Site coverageRoot) {
		this.cycles = cycles;
		this.locks = locks;
		this.threads = threads;
		this.emptyLockSetFields = emptyLockSetFields;
		this.threadCount = threadCount;
		this.objectCount = objectCount;
		this.classCount = classCount;
		this.root = coverageRoot;
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

	public Site getThreadCoverage() {
		return root;
	}

	public static class SummaryQuery implements DBQuery<SummaryInfo> {

		public SummaryInfo perform(final Query q) {
			List<Cycle> cycles = q.prepared("Deadlock.lockCycles",
					new DeadlockHandler(q)).call();
			List<Lock> locks = q.prepared("Deadlock.lockContention",
					new LockContentionHandler()).call();
			List<Thread> threads = q.prepared("SummaryInfo.threads",
					new ThreadContentionHandler()).call();
			Collections.sort(threads);
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
			Site root = new Site("", "", "");
			process(q,
					root,
					q.prepared("CoverageInfo.fieldCoverage",
							new CoverageHandler()).call());
			process(q,
					root,
					q.prepared("CoverageInfo.lockCoverage",
							new CoverageHandler()).call());
			return new SummaryInfo(cycles, locks, threads, fields, threadCount,
					objectCount, classCount, root);
		}

		void process(final Query q, final Site site,
				final Map<Long, Set<Long>> map) {
			for (Entry<Long, Set<Long>> e : map.entrySet()) {
				long traceId = e.getKey();
				LinkedList<Trace> trace = Trace.stackTrace(traceId).perform(q);
				Set<Long> threads = e.getValue();
				site.addTrace(trace.descendingIterator(), threads);
			}
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

	public static class Thread implements Comparable<Thread> {
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

		public int compareTo(final Thread o) {
			if (o == null) {
				return 1;
			}
			return name.compareTo(o.name);
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

		Set<String> getThreads() {
			Set<String> set = new HashSet<String>();
			for (Edge e : edges) {
				set.addAll(e.getThreads());
			}
			return set;
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
		private final List<String> threads;

		Edge(final String held, final String heldId, final String acquired,
				final String acquiredId, final int count,
				final Timestamp first, final Timestamp last,
				final List<String> threads) {
			this.held = held;
			this.heldId = heldId;
			this.acquired = acquired;
			this.acquiredId = acquiredId;
			this.count = count;
			this.first = first;
			this.last = last;
			this.threads = threads;
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

		public List<String> getThreads() {
			return threads;
		}

		public int compareTo(final Edge o) {
			int cmp = heldId.compareTo(o.heldId);
			if (cmp == 0) {
				cmp = acquiredId.compareTo(o.acquiredId);
			}
			return cmp;
		}

		@Override
		public String toString() {
			return "Edge [held=" + held + ", acquired=" + acquired + ", count="
					+ count + ", first=" + first + ", last=" + last + "]";
		}

	}

	private static class DeadlockHandler implements ResultHandler<List<Cycle>> {

		private final Query q;

		public DeadlockHandler(final Query q) {
			this.q = q;
		}

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
								first, last, q.prepared(
										"Deadlock.lockEdgeThreads",
										new StringRowHandler()).call(heldId,
										acquiredId)));
			}

			return cycles;
		}

	}

	static class Site implements Comparable<Site> {
		private final String pakkage;
		private final String clazz;
		private final String loc;
		private final Set<Long> threadsSeen;
		private final Map<Site, Site> children;

		public Site(final String pakkage, final String clazz, final String loc) {
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.loc = loc;
			threadsSeen = new HashSet<Long>();
			children = new TreeMap<Site, Site>();
		}

		public String getPackage() {
			return pakkage;
		}

		public String getClazz() {
			return clazz;
		}

		public String getLoc() {
			return loc;
		}

		public Set<Long> getThreadsSeen() {
			return threadsSeen;
		}

		public Set<Site> getChildren() {
			return children.keySet();
		}

		/**
		 * Update the sites along the given trace to include the given threads.
		 * 
		 * @param trace
		 *            the stack trace from farthest to nearest
		 * @param threads
		 */
		void addTrace(final Iterator<Trace> trace, final Set<Long> threads) {
			threadsSeen.addAll(threads);
			if (trace.hasNext()) {
				Trace t = trace.next();
				Site s = new Site(t.getPackage(), t.getClazz(), t.getLoc());
				Site child = children.get(s);
				if (child == null) {
					children.put(s, s);
					child = s;
				}
				child.addTrace(trace, threads);
			}
		}

		@Override
		public String toString() {
			return "Site [pakkage=" + pakkage + ", clazz=" + clazz + ", name="
					+ loc + "]";
		}

		public int compareTo(final Site o) {
			if (o == null) {
				return 1;
			}
			int cmp = pakkage.compareTo(o.pakkage);
			if (cmp == 0) {
				cmp = clazz.compareTo(o.clazz);
				if (cmp == 0) {
					cmp = loc.compareTo(o.loc);
				}
			}
			return cmp;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (clazz == null ? 0 : clazz.hashCode());
			result = prime * result + (loc == null ? 0 : loc.hashCode());
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
			Site other = (Site) obj;
			if (clazz == null) {
				if (other.clazz != null) {
					return false;
				}
			} else if (!clazz.equals(other.clazz)) {
				return false;
			}
			if (loc == null) {
				if (other.loc != null) {
					return false;
				}
			} else if (!loc.equals(other.loc)) {
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

	}

	private static class CoverageHandler implements
			ResultHandler<Map<Long, Set<Long>>> {
		public Map<Long, Set<Long>> handle(final Result result) {
			Map<Long, Set<Long>> map = new HashMap<Long, Set<Long>>();
			for (Row r : result) {
				long trace = r.nextLong();
				long thread = r.nextLong();
				Set<Long> set = map.get(trace);
				if (set == null) {
					set = new HashSet<Long>();
					map.put(trace, set);
				}
				set.add(thread);
			}
			return map;
		}
	}

}
