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

import com.surelogic.common.derby.sqlfunctions.Trace;
import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.LimitRowHandler;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.RowHandler;
import com.surelogic.common.jdbc.StringResultHandler;
import com.surelogic.common.jdbc.StringRowHandler;

public class SummaryInfo {

	private static final int CONTENTION_SITE_LIMIT = 100;

	private final List<Lock> locks;
	private final List<Thread> threads;

	private final List<LockSetEvidence> emptyLockSets;
	private final List<BadPublishEvidence> badPublishes;
	private final List<DeadlockEvidence> deadlocks;
	private final List<ContentionSite> contentionSites;
	private final String threadCount;
	private final String objectCount;
	private final String classCount;
	private final CoverageSite root;

	public SummaryInfo(final List<Lock> locks, final List<Thread> threads,
			final List<LockSetEvidence> emptyLockSetFields,
			final List<BadPublishEvidence> badPublishes,
			final List<DeadlockEvidence> deadlocks,
			final List<ContentionSite> contentionSites,
			final String objectCount, final String classCount,
			final CoverageSite coverageRoot) {
		this.locks = locks;
		this.threads = threads;
		this.threadCount = Integer.toString(threads.size());
		this.emptyLockSets = emptyLockSetFields;
		this.badPublishes = badPublishes;
		this.deadlocks = deadlocks;
		this.contentionSites = contentionSites;
		this.objectCount = objectCount;
		this.classCount = classCount;
		this.root = coverageRoot;
	}

	public List<Lock> getLocks() {
		return locks;
	}

	public List<Thread> getThreads() {
		return threads;
	}

	public List<LockSetEvidence> getEmptyLockSetFields() {
		return emptyLockSets;
	}

	public List<BadPublishEvidence> getBadPublishes() {
		return badPublishes;
	}

	public List<DeadlockEvidence> getDeadlocks() {
		return deadlocks;
	}

	public List<ContentionSite> getContentionSites() {
		return contentionSites;
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

	public CoverageSite getThreadCoverage() {
		return root;
	}

	public static class SummaryQuery implements DBQuery<SummaryInfo> {

		@Override
		public SummaryInfo perform(final Query q) {
			List<Lock> locks = q.prepared("Deadlock.lockContention",
					new LockContentionHandler()).call();
			List<Thread> threads = q.prepared("SummaryInfo.threads",
					new ThreadContentionHandler()).call();
			Collections.sort(threads);

			List<LockSetEvidence> emptyLockSets = new ArrayList<LockSetEvidence>();
			List<Field> nonstatics = q.prepared("SummaryInfo.emptyLockSets",
					new FieldHandler()).call();
			for (Field f : nonstatics) {
				emptyLockSets.add(q.prepared("SummaryInfo.likelyLocks",
						new LockSetEvidenceHandler(q, f))
						.call(f.id, f.id, f.id));
			}
			List<Field> statics = q.prepared("SummaryInfo.emptyStaticLockSets",
					new FieldHandler()).call();
			for (Field f : statics) {
				emptyLockSets.add(q.prepared("SummaryInfo.likelyLocks",
						new LockSetEvidenceHandler(q, f)).call(f.id));
			}
			Collections.sort(emptyLockSets);
			List<BadPublishEvidence> badPublishes = q.prepared(
					"SummaryInfo.badPublishes",
					new BadPublishEvidenceHandler(q)).call();
			List<DeadlockEvidence> deadlocks = q.prepared(
					"Deadlock.lockCycles", new DeadlockEvidenceHandler(q))
					.call();

			List<ContentionSite> contentionSites = q.prepared(
					"CoverageInfo.contentionHotSpots",
					LimitRowHandler.from(new ContentionSitesHandler(),
							CONTENTION_SITE_LIMIT)).call();

			String classCount = q.prepared("SummaryInfo.classCount",
					new StringResultHandler()).call();
			String objectCount = q.prepared("SummaryInfo.objectCount",
					new StringResultHandler()).call();
			CoverageSite root = new CoverageSite("", "", "", 0, "");
			process(q,
					root,
					q.prepared("CoverageInfo.fieldCoverage",
							new CoverageHandler()).call());
			process(q,
					root,
					q.prepared("CoverageInfo.lockCoverage",
							new CoverageHandler()).call());
			return new SummaryInfo(locks, threads, emptyLockSets, badPublishes,
					deadlocks, contentionSites, objectCount, classCount, root);
		}

		void process(final Query q, final CoverageSite site,
				final Map<Long, Set<Long>> map) {
			for (Entry<Long, Set<Long>> e : map.entrySet()) {
				long traceId = e.getKey();
				LinkedList<Trace> trace = Trace.stackTrace(traceId).perform(q);
				Set<Long> threads = e.getValue();
				site.addTrace(trace.descendingIterator(), threads);
			}
		}

	}

	public static class LockSetEvidence implements Comparable<LockSetEvidence>,
			Loc {
		private final Field field;
		private final List<LockSetLock> likelyLocks;

		public LockSetEvidence(final Field field) {
			this.field = field;
			this.likelyLocks = new ArrayList<LockSetLock>();
		}

		@Override
		public String getPackage() {
			return field.getPackage();
		}

		@Override
		public String getClazz() {
			return field.getClazz();
		}

		public String getName() {
			return field.getName();
		}

		public boolean isStatic() {
			return field.isStatic();
		}

		public long getId() {
			return field.getId();
		}

		public List<LockSetLock> getLikelyLocks() {
			return likelyLocks;
		}

		@Override
		public int compareTo(final LockSetEvidence o) {
			return field.compareTo(o.field);
		}

	}

	public static class LockSetLock {
		private final String name;
		private final long id;
		private final String timesAcquired;
		private final String heldPercentage;
		private final List<LockSetSite> heldAt;
		private final List<Site> notHeldAt;

		public LockSetLock(final String name, final long id,
				final String timesAcquired, final String heldPercentage) {
			this.name = name;
			this.id = id;
			this.timesAcquired = timesAcquired;
			this.heldPercentage = heldPercentage;
			this.heldAt = new ArrayList<LockSetSite>();
			this.notHeldAt = new ArrayList<Site>();
		}

		public String getName() {
			return name;
		}

		public long getId() {
			return id;
		}

		public String getHeldPercentage() {
			return heldPercentage;
		}

		public String getTimesAcquired() {
			return timesAcquired;
		}

		public List<LockSetSite> getHeldAt() {
			return heldAt;
		}

		public List<Site> getNotHeldAt() {
			return notHeldAt;
		}

		@Override
		public String toString() {
			return "LockSetLock [name=" + name + ", id=" + id
					+ ", heldPercentage=" + heldPercentage + ", acquisitions="
					+ heldAt + ", notHeldAt=" + notHeldAt + "]";
		}

	}

	/**
	 * Represents a site that has been accessed while a lock was held. It
	 * behaves the same as the field access site would, but has a getter to see
	 * where the lock was acquired.
	 * 
	 * @author nathan
	 * 
	 */
	public static class LockSetSite implements Comparable<LockSetSite>, Loc {

		private final Site access;
		private final Site acquiredAt;

		public LockSetSite(final Site access, final Site acquiredAt) {
			this.access = access;
			this.acquiredAt = acquiredAt;
		}

		public Site getAccess() {
			return access;
		}

		public Site getAcquiredAt() {
			return acquiredAt;
		}

		@Override
		public String getPackage() {
			return access.getPackage();
		}

		@Override
		public String getClazz() {
			return access.getClazz();
		}

		public String getLocation() {
			return access.getLocation();
		}

		public int getLine() {
			return access.getLine();
		}

		public String getFile() {
			return access.getFile();
		}

		@Override
		public String toString() {
			return access.toString();
		}

		@Override
		public int hashCode() {
			return access.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			return access.equals(obj);
		}

		@Override
		public int compareTo(final LockSetSite site) {
			return access.compareTo(site == null ? null : site.access);
		}

	}

	private static class LockSetEvidenceHandler implements
			ResultHandler<LockSetEvidence> {

		private static final int LOCK_LIMIT = 5;

		private final Field field;
		private final Query q;

		LockSetEvidenceHandler(final Query q, final Field f) {
			this.q = q;
			this.field = f;
		}

		@Override
		public LockSetEvidence handle(final Result result) {
			LockSetEvidence e = new LockSetEvidence(field);
			int count = 0;
			for (Row r : result) {
				if (count++ == LOCK_LIMIT) {
					return e;
				}
				LockSetLock l = new LockSetLock(r.nextString(), r.nextLong(),
						r.nextString(), r.nextString());
				if (field.isStatic()) {
					l.getHeldAt().addAll(
							q.prepared("SummaryInfo.lockHeldAt",
									new LockSetSiteHandler()).call(
									field.getId(), l.getId(), field.getId(),
									l.getId()));
					l.getNotHeldAt().addAll(
							q.prepared("SummaryInfo.lockNotHeldAt",
									new SiteHandler()).call(field.getId(),
									l.getId(), l.getId()));
				} else {
					l.getHeldAt().addAll(
							q.prepared("SummaryInfo.lockInstanceHeldAt",
									new LockSetSiteHandler()).call(
									field.getId(), l.getId(), field.getId(),
									l.getId()));
					l.getNotHeldAt().addAll(
							q.prepared("SummaryInfo.lockInstanceNotHeldAt",
									new SiteHandler()).call(field.getId(),
									l.getId(), l.getId()));
				}
				e.getLikelyLocks().add(l);
			}
			return e;
		}

	}

	public static class DeadlockEvidence {
		private final Cycle cycle;
		private final List<DeadlockTrace> traces;

		public DeadlockEvidence(final Cycle cycle) {
			this.cycle = cycle;
			this.traces = new ArrayList<SummaryInfo.DeadlockTrace>();
		}

		public Cycle getCycle() {
			return cycle;
		}

		public List<DeadlockTrace> getTraces() {
			return traces;
		}

	}

	/**
	 * Represents a single trace that contributes an edge to the deadlock graph.
	 * 
	 * @author nathan
	 * 
	 */
	public static class DeadlockTrace {
		private final Edge edge;
		private final List<Trace> trace;
		private final List<LockTrace> lockTrace;

		public DeadlockTrace(final Edge edge, final List<Trace> trace,
				final List<LockTrace> lockTrace) {
			this.edge = edge;
			this.trace = trace;
			this.lockTrace = lockTrace;
		}

		/**
		 * The edge this trace contributed to the deadlock graph
		 * 
		 * @return
		 */
		public Edge getEdge() {
			return edge;
		}

		/**
		 * The stack trace for this lock acquisition.
		 * 
		 * @return
		 */
		public List<Trace> getTrace() {
			return trace;
		}

		/**
		 * The lock trace for this lock acquisition, from most to least recently
		 * acquired.
		 * 
		 * @return
		 */
		public List<LockTrace> getLockTrace() {
			return lockTrace;
		}

	}

	public static class LockTrace {
		private final String lock;
		private final long id;
		private final String pakkage;
		private final String clazz;
		private final int line;

		public LockTrace(final long id, final String lock,
				final String pakkage, final String clazz, final int line) {
			this.id = id;
			this.lock = lock;
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.line = line;
		}

		public String getId() {
			return Long.toString(id);
		}

		public String getLock() {
			return lock;
		}

		public String getPackage() {
			return pakkage;
		}

		public String getClazz() {
			return clazz;
		}

		public int getLine() {
			return line;
		}

	}

	static class LockTraceHandler implements RowHandler<LockTrace> {
		@Override
		public LockTrace handle(final Row r) {
			return new LockTrace(r.nextLong(), r.nextString(), r.nextString(),
					r.nextString(), r.nextInt());
		}
	}

	public static class ContentionSite {
		private final Site s;
		private final long durationNs;

		public ContentionSite(final Site s, final long durationNs) {
			this.s = s;
			this.durationNs = durationNs;
		}

		public Site getSite() {
			return s;
		}

		public long getDurationNs() {
			return durationNs;
		}

	}

	private static class ContentionSitesHandler implements
			RowHandler<ContentionSite> {
		private final SiteHandler sh = new SiteHandler();

		@Override
		public ContentionSite handle(final Row r) {
			long duration = r.nextLong();
			Site site = sh.handle(r);
			return new ContentionSite(site, duration);
		}

	}

	private static class BadPublishEvidenceHandler implements
			RowHandler<BadPublishEvidence> {

		private final Query q;

		BadPublishEvidenceHandler(final Query q) {
			this.q = q;
		}

		@Override
		public BadPublishEvidence handle(final Row r) {
			Field f = new Field(r.nextString(), r.nextString(), r.nextString(),
					r.nextLong(), r.nextBoolean());
			final BadPublishEvidence e = new BadPublishEvidence(f);
			q.prepared("SummaryInfo.underConstructionAccesses",
					new NullRowHandler() {
						@Override
						protected void doHandle(final Row r) {
							String thread = r.nextString();
							boolean isRead = r.nextBoolean();
							Timestamp time = r.nextTimestamp();
							List<Trace> trace = Trace.stackTrace(r.nextLong())
									.perform(q);
							e.getAccesses().add(
									new BadPublishAccess(thread, isRead, time,
											trace));
						}
					}).call(f.getId());
			return e;
		}
	}

	public static class BadPublishEvidence implements Loc {
		private final Field f;
		private final List<BadPublishAccess> accesses;

		public BadPublishEvidence(final Field f) {
			this.f = f;
			accesses = new ArrayList<BadPublishAccess>();
		}

		@Override
		public String getPackage() {
			return f.getPackage();
		}

		@Override
		public String getClazz() {
			return f.getClazz();
		}

		public String getName() {
			return f.getName();
		}

		public long getId() {
			return f.getId();
		}

		public boolean isStatic() {
			return f.isStatic();
		}

		public List<BadPublishAccess> getAccesses() {
			return accesses;
		}

	}

	public static class BadPublishAccess {
		private final String thread;
		private final boolean isRead;
		private final Timestamp time;
		private final List<Trace> trace;

		public BadPublishAccess(final String thread, final boolean isRead,
				final Timestamp time, final List<Trace> trace) {
			this.thread = thread;
			this.isRead = isRead;
			this.time = time;
			this.trace = trace;
		}

		public String getThread() {
			return thread;
		}

		public boolean isRead() {
			return isRead;
		}

		public Timestamp getTime() {
			return time;
		}

		public List<Trace> getTrace() {
			return trace;
		}

	}

	public interface Loc {
		String getPackage();

		String getClazz();

	}

	public static class Field implements Comparable<Field>, Loc {
		private final String pakkage;
		private final String clazz;
		private final String name;
		private final long id;
		private final boolean isStatic;

		public Field(final String pakkage, final String clazz,
				final String name, final long id, final boolean isStatic) {
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.name = name;
			this.id = id;
			this.isStatic = isStatic;
		}

		@Override
		public String getPackage() {
			return pakkage;
		}

		@Override
		public String getClazz() {
			return clazz;
		}

		public String getName() {
			return name;
		}

		public long getId() {
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

		@Override
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

		@Override
		public String toString() {
			return "Field [pakkage=" + pakkage + ", clazz=" + clazz + ", name="
					+ name + ", id=" + id + ", isStatic=" + isStatic + "]";
		}

	}

	private static class FieldHandler implements RowHandler<Field> {

		@Override
		public Field handle(final Row r) {
			return new Field(r.nextString(), r.nextString(), r.nextString(),
					r.nextLong(), r.nextBoolean());
		}

	}

	public static class Thread implements Comparable<Thread> {
		private final String id;
		private final String name;
		private final Date start;
		private final Date stop;
		private final long blockTime;

		public Thread(final String id, final String name, final Date start,
				final Date stop, final long blockTime) {
			this.id = id;
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

		@Override
		public int compareTo(final Thread o) {
			if (o == null) {
				return 1;
			}
			return name.compareTo(o.name);
		}

		public String getId() {
			return id;
		}

	}

	private static class ThreadContentionHandler implements RowHandler<Thread> {

		@Override
		public Thread handle(final Row r) {
			return new Thread(r.nextString(), r.nextString(),
					r.nextTimestamp(), r.nextTimestamp(), r.nextLong());
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

		@Override
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

		@Override
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

	private static class DeadlockEvidenceHandler implements
			ResultHandler<List<DeadlockEvidence>> {

		private final Query q;

		public DeadlockEvidenceHandler(final Query q) {
			this.q = q;
		}

		@Override
		public List<DeadlockEvidence> handle(final Result result) {
			List<DeadlockEvidence> deadlocks = new ArrayList<DeadlockEvidence>();
			DeadlockEvidence deadlock = null;
			Cycle curCycle = new Cycle(-1);
			for (final Row r : result) {
				final int cycle = r.nextInt();
				if (curCycle.getNum() != cycle) {
					curCycle = new Cycle(cycle);
					deadlock = new DeadlockEvidence(curCycle);
					deadlocks.add(deadlock);
				}
				final String held = r.nextString();
				final String heldId = r.nextString();
				final String acquired = r.nextString();
				final String acquiredId = r.nextString();
				final int count = r.nextInt();
				final Timestamp first = r.nextTimestamp();
				final Timestamp last = r.nextTimestamp();
				Edge e = new Edge(held, heldId, acquired, acquiredId, count,
						first, last, q.prepared("Deadlock.lockEdgeThreads",
								new StringRowHandler())
								.call(heldId, acquiredId));
				curCycle.getEdges().add(e);
				DeadlockTrace dt = q.prepared("Deadlock.lockEdgeTraces",
						new DeadlockTraceHandler(q, e))
						.call(heldId, acquiredId);
				deadlock.getTraces().add(dt);
			}
			return deadlocks;
		}

	}

	private static class DeadlockTraceHandler implements
			ResultHandler<DeadlockTrace> {
		private final Query q;
		private final Edge edge;

		DeadlockTraceHandler(final Query q, final Edge e) {
			this.q = q;
			this.edge = e;
		}

		@Override
		public DeadlockTrace handle(final Result result) {
			for (Row r : result) {
				long traceId = r.nextLong();
				long lockEventId = r.nextLong();
				List<Trace> trace = Trace.stackTrace(traceId).perform(q);
				List<LockTrace> lockTrace = q.prepared(
						"Deadlock.lockEdgeLockTrace", new LockTraceHandler())
						.call(lockEventId);
				return new DeadlockTrace(edge, trace, lockTrace);
			}
			return null;
		}

	}

	private static class SiteHandler implements RowHandler<Site> {
		@Override
		public Site handle(final Row r) {
			return new Site(r.nextString(), r.nextString(), r.nextString(),
					r.nextInt(), r.nextString());
		}
	}

	private static class LockSetSiteHandler implements RowHandler<LockSetSite> {
		private static final SiteHandler sh = new SiteHandler();

		@Override
		public LockSetSite handle(final Row r) {
			// TODO Add a query to find where this lock was acquired
			return new LockSetSite(sh.handle(r), null);
		}

	}

	public static class Site implements Comparable<Site>, Loc {
		private final String pakkage;
		private final String clazz;
		private final String location;
		private final int line;
		private final String file;

		public Site(final String pakkage, final String clazz,
				final String location, final int line, final String file) {
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.location = location;
			this.line = line;
			this.file = file;
		}

		@Override
		public String getPackage() {
			return pakkage;
		}

		@Override
		public String getClazz() {
			return clazz;
		}

		public String getLocation() {
			return location;
		}

		public int getLine() {
			return line;
		}

		public String getFile() {
			return file;
		}

		@Override
		public String toString() {
			return "Site [pakkage=" + pakkage + ", clazz=" + clazz + ", name="
					+ location + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (clazz == null ? 0 : clazz.hashCode());
			result = prime * result + (file == null ? 0 : file.hashCode());
			result = prime * result + line;
			result = prime * result
					+ (location == null ? 0 : location.hashCode());
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
			if (file == null) {
				if (other.file != null) {
					return false;
				}
			} else if (!file.equals(other.file)) {
				return false;
			}
			if (line != other.line) {
				return false;
			}
			if (location == null) {
				if (other.location != null) {
					return false;
				}
			} else if (!location.equals(other.location)) {
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

		@Override
		public int compareTo(final Site site) {
			if (site == null) {
				return 1;
			}
			int cmp = pakkage.compareTo(site.pakkage);
			if (cmp == 0) {
				cmp = clazz.compareTo(site.clazz);
				if (cmp == 0) {
					cmp = location.compareTo(site.location);
				}
			}
			return cmp;
		}

	}

	public static class CoverageSite implements Comparable<CoverageSite> {

		private final Site site;
		private final Set<Long> threadsSeen;
		private final Map<CoverageSite, CoverageSite> children;

		public CoverageSite(final String pakkage, final String clazz,
				final String location, final int line, final String file) {
			site = new Site(pakkage, clazz, location, line, file);
			threadsSeen = new HashSet<Long>();
			children = new TreeMap<CoverageSite, CoverageSite>();
		}

		public Set<Long> getThreadsSeen() {
			return threadsSeen;
		}

		public Set<CoverageSite> getChildren() {
			return children.keySet();
		}

		public String getPackage() {
			return site.getPackage();
		}

		public String getClazz() {
			return site.getClazz();
		}

		public String getLocation() {
			return site.getLocation();
		}

		public int getLine() {
			return site.getLine();
		}

		public String getFile() {
			return site.getFile();
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
				CoverageSite s = new CoverageSite(t.getPackage(), t.getClazz(),
						t.getLoc(), t.getLine(), t.getFile());
				CoverageSite child = children.get(s);
				if (child == null) {
					children.put(s, s);
					child = s;
				}
				child.addTrace(trace, threads);
			}
		}

		@Override
		public String toString() {
			return site.toString();
		}

		@Override
		public int compareTo(final CoverageSite o) {
			if (o == null) {
				return 1;
			}
			return site.compareTo(o.site);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (site == null ? 0 : site.hashCode());
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
			CoverageSite other = (CoverageSite) obj;
			if (site == null) {
				if (other.site != null) {
					return false;
				}
			} else if (!site.equals(other.site)) {
				return false;
			}
			return true;
		}

	}

	private static class CoverageHandler implements
			ResultHandler<Map<Long, Set<Long>>> {
		@Override
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
