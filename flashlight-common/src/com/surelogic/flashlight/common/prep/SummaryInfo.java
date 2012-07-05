package com.surelogic.flashlight.common.prep;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
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
import com.surelogic.common.jdbc.LimitedResult;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.RowHandler;
import com.surelogic.common.jdbc.StringResultHandler;
import com.surelogic.common.jdbc.StringRowHandler;

/**
 * Used by WriteHtmlOverview, this class provides a wide-ranging summary of all
 * of the data collected by the postmortem tool. We make an attempt to bound the
 * data by program size - that is, it is acceptable for this summary to grow
 * larger as more fields, classes, or methods are added to the program, but it
 * is not acceptable for it to grow larger based on how long the program is run,
 * or how many locks and threads it creates at runtime.
 * 
 * @author nathan
 * 
 */
public class SummaryInfo {

	/*
	 * # Locks shown on Lock Contention page
	 */
	private static final int LOCK_LIMIT = 75;
	/*
	 * # Threads shown on Coverage page
	 */
	private static final int THREAD_LIMIT = 100;
	/*
	 * # Lock Cycles shown on Deadlocks page
	 */
	private static final int LOCK_CYCLE_LIMIT = 20;

	private final LimitedResult<Lock> locks;
	private final LimitedResult<Thread> threads;
	private final LimitedResult<DeadlockEvidence> deadlocks;

	private final List<LockSetEvidence> emptyLockSets;
	private final List<BadPublishEvidence> badPublishes;

	private final List<FieldCoverage> fields;

	private final String objectCount;
	private final String classCount;

	private final CoverageSite root;

	public SummaryInfo(final LimitedResult<Lock> locks,
			final LimitedResult<Thread> threads,
			final List<LockSetEvidence> emptyLockSetFields,
			final List<BadPublishEvidence> badPublishes,
			final LimitedResult<DeadlockEvidence> deadlocks,
			final String objectCount, final String classCount,
			final CoverageSite coverageRoot, final List<FieldCoverage> fields) {
		this.locks = locks;
		this.threads = threads;
		emptyLockSets = emptyLockSetFields;
		this.badPublishes = badPublishes;
		this.deadlocks = deadlocks;
		this.objectCount = objectCount;
		this.classCount = classCount;
		root = coverageRoot;
		this.fields = fields;
	}

	public LimitedResult<Lock> getLocks() {
		return locks;
	}

	public LimitedResult<Thread> getThreads() {
		return threads;
	}

	public List<LockSetEvidence> getEmptyLockSetFields() {
		return emptyLockSets;
	}

	public List<BadPublishEvidence> getBadPublishes() {
		return badPublishes;
	}

	public LimitedResult<DeadlockEvidence> getDeadlocks() {
		return deadlocks;
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

	public List<FieldCoverage> getFields() {
		return fields;
	}

	public static class SummaryQuery implements DBQuery<SummaryInfo> {

		@Override
		public SummaryInfo perform(final Query q) {
			LimitedResult<Lock> locks = q.prepared(
					"Deadlock.lockContention",
					LimitRowHandler.from(new LockContentionHandler(q),
							LOCK_LIMIT)).call();
			LimitedResult<Thread> threads = q.prepared(
					"SummaryInfo.threads",
					LimitRowHandler.from(new ThreadContentionHandler(),
							THREAD_LIMIT)).call();
			Collections.sort(threads);

			List<LockSetEvidence> emptyLockSets = new ArrayList<LockSetEvidence>();
			List<Field> nonstatics = q.prepared("SummaryInfo.emptyLockSets",
					new FieldHandler()).call();
			for (Field f : nonstatics) {
				LockSetEvidence e = new LockSetEvidence(f, q.prepared(
						"SummaryInfo.likelyLocks",
						LimitRowHandler.from(new LockSetEvidenceHandler(q, f),
								LOCK_LIMIT)).call(f.id, f.receiver, f.id,
						f.receiver, f.id, f.receiver));
				emptyLockSets.add(e);
			}
			List<Field> statics = q.prepared("SummaryInfo.emptyStaticLockSets",
					new FieldHandler()).call();
			for (Field f : statics) {
				LockSetEvidence e = new LockSetEvidence(f, q.prepared(
						"SummaryInfo.likelyStaticLocks",
						LimitRowHandler.from(new LockSetEvidenceHandler(q, f),
								LOCK_LIMIT)).call(f.id, f.id, f.id));
				emptyLockSets.add(e);
			}
			Collections.sort(emptyLockSets);
			List<BadPublishEvidence> badPublishes = q.prepared(
					"SummaryInfo.badPublishes",
					new BadPublishEvidenceHandler(q)).call();
			LimitedResult<DeadlockEvidence> deadlocks = q.prepared(
					"Deadlock.lockCycles", new DeadlockEvidenceHandler(q))
					.call();
			String classCount = q.prepared("SummaryInfo.classCount",
					new StringResultHandler()).call();
			String objectCount = q.prepared("SummaryInfo.objectCount",
					new StringResultHandler()).call();
			CoverageSite root = new CoverageSite();
			process(q,
					root,
					q.prepared("CoverageInfo.fieldCoverage",
							new CoverageHandler()).call());
			process(q,
					root,
					q.prepared("CoverageInfo.lockCoverage",
							new CoverageHandler()).call());
			List<FieldCoverage> fields = new ArrayList<FieldCoverage>();
			fields.addAll(q.prepared("SummaryInfo.staticFieldCoverage",
					new FieldCoverageHandler()).call());
			return new SummaryInfo(locks, threads, emptyLockSets, badPublishes,
					deadlocks, objectCount, classCount, root, fields);
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
		private final LimitedResult<LockSetLock> likelyLocks;

		public LockSetEvidence(final Field field,
				final LimitedResult<LockSetLock> likelyLocks) {
			this.field = field;
			this.likelyLocks = likelyLocks;
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

		public LimitedResult<LockSetLock> getLikelyLocks() {
			return likelyLocks;
		}

		public Long getReceiver() {
			return field.getReceiver();
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
			heldAt = new ArrayList<LockSetSite>();
			notHeldAt = new ArrayList<Site>();
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
			RowHandler<LockSetLock> {

		private final Field field;
		private final Query q;

		LockSetEvidenceHandler(final Query q, final Field f) {
			this.q = q;
			field = f;
		}

		@Override
		public LockSetLock handle(final Row r) {
			LockSetLock l = new LockSetLock(r.nextString(), r.nextLong(),
					r.nextString(), r.nextString());
			if (field.isStatic()) {
				l.getHeldAt().addAll(
						q.prepared("SummaryInfo.lockHeldAt",
								new LockSetSiteHandler()).call(field.getId(),
								l.getId(), field.getId(), l.getId()));
				l.getNotHeldAt().addAll(
						q.prepared("SummaryInfo.lockNotHeldAt",
								new SiteHandler()).call(field.getId(),
								l.getId(), l.getId()));
			} else {
				l.getHeldAt().addAll(
						q.prepared("SummaryInfo.lockInstanceHeldAt",
								new LockSetSiteHandler()).call(field.getId(),
								field.getReceiver(), l.getId(), field.getId(),
								field.getReceiver(), l.getId()));
				l.getNotHeldAt().addAll(
						q.prepared("SummaryInfo.lockInstanceNotHeldAt",
								new SiteHandler()).call(field.getId(),
								field.getReceiver(), l.getId(), l.getId()));
			}
			return l;
		}

	}

	/**
	 * An evidence trail for a deadlock. It consists of a set of lock edges that
	 * could potentially cause a deadlock, and supporting evidence for each edge
	 * in the form of a stack trace. The stack trace shows where the lock was
	 * acquired.
	 * 
	 * @author nathan
	 * 
	 */
	public static class DeadlockEvidence {

		private Map<Edge, DeadlockTrace> traces;
		private Set<Edge> edges;
		private final int num;

		public DeadlockEvidence(final int num) {
			this.num = num;
		}

		public Set<Edge> getEdges() {
			if (edges == null) {
				edges = new TreeSet<SummaryInfo.Edge>();
			}
			return edges;
		}

		public int getNum() {
			return num;
		}

		/**
		 * The set of locks contributing to this deadlock.
		 * 
		 * @return
		 */
		Set<String> getLocks() {
			final Set<String> locks = new TreeSet<String>();
			for (final Edge e : getEdges()) {
				locks.add(e.getHeld());
				locks.add(e.getAcquired());
			}
			return locks;
		}

		/**
		 * The set of threads contributing to this deadlock.
		 * 
		 * @return
		 */
		Set<String> getThreads() {
			Set<String> set = new HashSet<String>();
			for (Edge e : getEdges()) {
				set.addAll(e.getThreads());
			}
			return set;
		}

		public void addTrace(final DeadlockTrace trace) {
			getTraces().put(trace.getEdge(), trace);
		}

		public DeadlockTrace getTrace(final Edge e) {
			return getTraces().get(e);
		}

		public Map<Edge, DeadlockTrace> getTraces() {
			if (traces == null) {
				traces = new HashMap<SummaryInfo.Edge, SummaryInfo.DeadlockTrace>();
			}
			return traces;
		}
	}

	/**
	 * Represents a single trace that contributes an edge to the deadlock graph.
	 * 
	 * @author nathan
	 * 
	 */
	public static class DeadlockTrace implements Comparable<DeadlockTrace> {
		private final Edge edge;
		private final List<Trace> trace;
		private final List<LockTrace> lockTrace;
		private final List<Trace> heldTrace;

		public DeadlockTrace(final Edge edge, final List<Trace> trace,
				final List<Trace> heldTrace, final List<LockTrace> lockTrace) {
			this.edge = edge;
			this.trace = trace;
			this.heldTrace = heldTrace;
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
		 * The stack trace for the acquired lock's acquisition.
		 * 
		 * @return
		 */
		public List<Trace> getTrace() {
			return trace;
		}

		/**
		 * The stack trace for the held lock's acquisition.
		 * 
		 * @return
		 */
		public List<Trace> getHeldTrace() {
			return heldTrace;
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

		@Override
		public int compareTo(final DeadlockTrace o) {
			return edge.compareTo(o.getEdge());
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

	public static class ThreadDuration {
		private final String threadId;
		private final long durationNs;

		public String getThreadId() {
			return threadId;
		}

		public ThreadDuration(final String threadId, final long durationNs) {
			this.threadId = threadId;
			this.durationNs = durationNs;
		}

		public long getDurationNs() {
			return durationNs;
		}

	}

	/**
	 * The site of some amount of lock contention.
	 * 
	 * @author nathan
	 * 
	 */
	public static class ContentionSite {
		private final Site s;
		private final List<ThreadDuration> durations;

		public ContentionSite(final Site s) {
			this.s = s;
			durations = new ArrayList<ThreadDuration>();
		}

		public Site getSite() {
			return s;
		}

		public long getDurationNs() {
			long durationNs = 0;
			for (ThreadDuration duration : durations) {
				durationNs += duration.getDurationNs();
			}
			return durationNs;
		}

		public List<ThreadDuration> getDurations() {
			return durations;
		}

	}

	private static class ContentionSitesHandler implements
			ResultHandler<List<ContentionSite>> {
		private final SiteHandler sh = new SiteHandler();

		@Override
		public List<ContentionSite> handle(final Result result) {
			List<ContentionSite> sites = new ArrayList<ContentionSite>();
			ContentionSite cs = null;
			Site s = null;
			for (Row r : result) {
				final long duration = r.nextLong();
				final Site site = sh.handle(r);
				final String thread = r.nextString();
				if (!site.equals(s)) {
					s = site;
					if (cs != null) {
						sites.add(cs);
					}
					cs = new ContentionSite(s);
				}
				cs.getDurations().add(new ThreadDuration(thread, duration));
			}
			if (cs != null) {
				sites.add(cs);
			}
			return sites;
		}
	}

	private static class BadPublishEvidenceHandler implements
			RowHandler<BadPublishEvidence> {
		private static final int BAD_PUBLISH_ACCESS_LIMIT = 50;
		private final Query q;
		private final FieldHandler handler = new FieldHandler();

		BadPublishEvidenceHandler(final Query q) {
			this.q = q;
		}

		@Override
		public BadPublishEvidence handle(final Row r) {
			Field f = handler.handle(r);
			final BadPublishEvidence e = new BadPublishEvidence(f,

			q.prepared("SummaryInfo.underConstructionAccesses",
					LimitRowHandler.from(new RowHandler<BadPublishAccess>() {
						@Override
						public BadPublishAccess handle(final Row r) {
							String thread = r.nextString();
							boolean isRead = r.nextBoolean();
							Timestamp time = r.nextTimestamp();
							List<Trace> trace = Trace.stackTrace(r.nextLong())
									.perform(q);

							return new BadPublishAccess(thread, isRead, time,
									trace);
						}
					}, BAD_PUBLISH_ACCESS_LIMIT)).call(f.getId(),
					f.getReceiver()));
			return e;
		}
	}

	public static class BadPublishEvidence implements Loc {
		private final Field f;
		private final LimitedResult<BadPublishAccess> accesses;

		public BadPublishEvidence(final Field f,
				final LimitedResult<BadPublishAccess> accesses) {
			this.f = f;
			this.accesses = accesses;
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

		public Long getReceiver() {
			return f.getReceiver();
		}

		public LimitedResult<BadPublishAccess> getAccesses() {
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
		private final Long receiver;

		public Field(final String pakkage, final String clazz,
				final String name, final long id, final boolean isStatic,
				final Long receiver) {
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.name = name;
			this.id = id;
			this.isStatic = isStatic;
			this.receiver = receiver;
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

		public Long getReceiver() {
			return receiver;
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
			String pakkage = r.nextString();
			String clazz = r.nextString();
			String field = r.nextString();
			long id = r.nextLong();
			boolean isStatic = r.nextBoolean();
			Long receiver = null;
			if (!isStatic) {
				receiver = r.nullableLong();
			}
			return new Field(pakkage, clazz, field, id, isStatic, receiver);
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
		private final String acquired;
		private final long blockTime;
		private final long averageBlock;

		private final List<ContentionSite> contentionSites;

		public Lock(final String name, final int acquired,
				final long blockTime, final long averageBlock, final long id) {
			this.id = Long.toString(id);
			this.name = name;
			this.acquired = Integer.toString(acquired);
			this.blockTime = blockTime;
			this.averageBlock = averageBlock;
			contentionSites = new ArrayList<SummaryInfo.ContentionSite>();
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		public String getAcquired() {
			return acquired;
		}

		public long getBlockTime() {
			return blockTime;
		}

		public long getAverageBlock() {
			return averageBlock;
		}

		public List<ContentionSite> getContentionSites() {
			return contentionSites;
		}

	}

	private static class LockContentionHandler implements RowHandler<Lock> {

		private final Queryable<List<ContentionSite>> prepared;

		public LockContentionHandler(final Query q) {
			prepared = q.prepared("SummaryInfo.lockContentionSites",
					new ContentionSitesHandler());
		}

		@Override
		public Lock handle(final Row r) {
			Lock l = new Lock(r.nextString(), r.nextInt(), r.nextLong(),
					r.nextLong(), r.nextLong());
			l.getContentionSites().addAll(prepared.call(l.getId()));
			return l;
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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (acquiredId == null ? 0 : acquiredId.hashCode());
			result = prime * result + (heldId == null ? 0 : heldId.hashCode());
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
			Edge other = (Edge) obj;
			if (acquiredId == null) {
				if (other.acquiredId != null) {
					return false;
				}
			} else if (!acquiredId.equals(other.acquiredId)) {
				return false;
			}
			if (heldId == null) {
				if (other.heldId != null) {
					return false;
				}
			} else if (!heldId.equals(other.heldId)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "Edge [held=" + held + ", acquired=" + acquired + ", count="
					+ count + ", first=" + first + ", last=" + last + "]";
		}

	}

	/**
	 * Constructs a lock cycle graph, and adds supporting evidence for the graph
	 * in the form of stack traces that lead to particular lock acquisitions.
	 * 
	 * @author nathan
	 * 
	 */
	private static class DeadlockEvidenceHandler implements
			ResultHandler<LimitedResult<DeadlockEvidence>> {

		private final Query q;

		public DeadlockEvidenceHandler(final Query q) {
			this.q = q;
		}

		@Override
		public LimitedResult<DeadlockEvidence> handle(final Result result) {
			List<DeadlockEvidence> deadlocks = new ArrayList<DeadlockEvidence>();
			DeadlockEvidence deadlock = new DeadlockEvidence(-1);
			int cycleCount = 0;
			for (final Row r : result) {
				final int cycle = r.nextInt();
				if (deadlock.getNum() != cycle) {
					deadlock = new DeadlockEvidence(cycle);
					if (++cycleCount >= LOCK_CYCLE_LIMIT) {
						continue;
					}
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
				deadlock.getEdges().add(e);
				DeadlockTrace dt = q.prepared("Deadlock.lockEdgeTraces",
						new DeadlockTraceHandler(q, e))
						.call(heldId, acquiredId);
				deadlock.addTrace(dt);
			}
			return new LimitedResult<SummaryInfo.DeadlockEvidence>(deadlocks,
					cycleCount);
		}
	}

	private static class DeadlockTraceHandler implements
			ResultHandler<DeadlockTrace> {
		private final Query q;
		private final Edge edge;

		DeadlockTraceHandler(final Query q, final Edge e) {
			this.q = q;
			edge = e;
		}

		@Override
		public DeadlockTrace handle(final Result result) {
			for (Row r : result) {
				long traceId = r.nextLong();
				long lockEventId = r.nextLong();
				long heldTraceId = r.nextLong();
				List<Trace> trace = Trace.stackTrace(traceId).perform(q);
				List<Trace> heldTrace = Trace.stackTrace(heldTraceId)
						.perform(q);
				List<LockTrace> lockTrace = q.prepared(
						"Deadlock.lockEdgeLockTrace", new LockTraceHandler())
						.call(lockEventId);
				return new DeadlockTrace(edge, trace, heldTrace, lockTrace);
			}
			return null;
		}

	}

	private static class SiteHandler implements RowHandler<Site> {
		@Override
		public Site handle(final Row r) {
			return new Site(r.nextString(), r.nextString(), r.nextString(),
					r.nextInt(), r.nextString(), r.nextString(), r.nextString());
		}
	}

	private static class LockSetSiteHandler implements RowHandler<LockSetSite> {
		private static final SiteHandler sh = new SiteHandler();

		@Override
		public LockSetSite handle(final Row r) {
			return new LockSetSite(sh.handle(r), sh.handle(r));
		}

	}

	public static class Site implements Comparable<Site>, Loc {
		private final String pakkage;
		private final String clazz;
		private final String location;
		private final int line;
		private final String file;
		private final String methodCallClass;
		private final String methodCallName;

		public Site(final String pakkage, final String clazz,
				final String location, final int line, final String file) {
			this(pakkage, clazz, location, line, file, null, null);
		}

		public Site(final String pakkage, final String clazz,
				final String location, final int line, final String file,
				final String methodCallClass, String methodCallName) {
			this.pakkage = pakkage;
			this.clazz = clazz;
			this.location = location;
			this.line = line;
			this.file = file;
			this.methodCallClass = methodCallClass;
			this.methodCallName = methodCallName;
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

		public String getMethodCallClass() {
			return methodCallClass;
		}

		public String getMethodCallName() {
			return methodCallName;
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

		public boolean equalsByLocation(final Object obj) {
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

		public int compareToByLocation(final Site site) {
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
					if (cmp == 0) {
						cmp = line < site.line ? -1 : line == site.line ? 0 : 1;
						if (cmp == 0) {
							cmp = file.compareTo(site.file);
						}
					}
				}
			}
			return cmp;
		}

	}

	public static class CoverageSite implements Comparable<CoverageSite> {

		private final Site site;
		private final Set<Long> threadsSeen;
		private final Map<Site, CoverageSite> children;

		public CoverageSite(final Site site) {
			this.site = site;
			threadsSeen = new HashSet<Long>();
			children = new TreeMap<Site, CoverageSite>();
		}

		public CoverageSite() {
			this(
					new Site("*root*", "*root*", "*root*", 0, "*root*", null,
							null));
		}

		public Site getSite() {
			return site;
		}

		public Set<Long> getThreadsSeen() {
			return threadsSeen;
		}

		public Collection<CoverageSite> getChildren() {
			return children.values();
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
			if (trace.hasNext()) {
				addTraceHelper(trace.next(), trace, threads);
			}
		}

		void addTraceHelper(final Trace t, final Iterator<Trace> rest,
				final Set<Long> threads) {

			Site s = new Site(t.getPackage(), t.getClazz(), t.getLoc(),
					t.getLine(), t.getFile(), t.getMethodCallClass(),
					t.getMethodCallName());
			CoverageSite child = children.get(s);
			if (child == null) {
				child = new CoverageSite(s);
				children.put(s, child);

			}
			child.threadsSeen.addAll(threads);
			if (rest.hasNext()) {
				// We don't want to show the full depth of recursive method
				// invocations
				Trace nextT = null;
				Site nextS = s;
				while (s.equals(nextS) && rest.hasNext()) {
					nextT = rest.next();
					nextS = new Site(nextT.getPackage(), nextT.getClazz(),
							nextT.getLoc(), nextT.getLine(), nextT.getFile(),
							nextT.getMethodCallClass(),
							nextT.getMethodCallName());
				}
				if (nextT != null && !s.equals(nextS)) {
					child.addTraceHelper(nextT, rest, threads);
				}
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
			return site.compareToByLocation(o.site);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (children == null ? 0 : children.hashCode());
			result = prime * result + (site == null ? 0 : site.hashCode());
			result = prime * result
					+ (threadsSeen == null ? 0 : threadsSeen.hashCode());
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
			if (children == null) {
				if (other.children != null) {
					return false;
				}
			} else if (!children.equals(other.children)) {
				return false;
			}
			if (site == null) {
				if (other.site != null) {
					return false;
				}
			} else if (!site.equals(other.site)) {
				return false;
			}
			if (threadsSeen == null) {
				if (other.threadsSeen != null) {
					return false;
				}
			} else if (!threadsSeen.equals(other.threadsSeen)) {
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

	public static class FieldCoverage {
		private final Field field;
		private final Set<Long> threadsSeen;

		public FieldCoverage(final Field field) {
			this.field = field;
			threadsSeen = new HashSet<Long>();
		}

		public String getClazz() {
			return field.getClazz();
		}

		public String getPackage() {
			return field.getPackage();
		}

		public String getName() {
			return field.getName();
		}

		public long getId() {
			return field.getId();
		}

		public boolean isStatic() {
			return field.isStatic();
		}

		public Long getReceiver() {
			return field.getReceiver();
		}

		public Set<Long> getThreadsSeen() {
			return threadsSeen;
		}

	}

	private static class FieldCoverageHandler implements
			ResultHandler<List<FieldCoverage>> {

		@Override
		public List<FieldCoverage> handle(final Result result) {
			Field current = null;
			FieldCoverage currentCov = null;
			List<FieldCoverage> fields = new ArrayList<FieldCoverage>();
			for (Row r : result) {
				long id = r.nextLong();
				String pakkage = r.nextString();
				String clazz = r.nextString();
				String name = r.nextString();
				boolean isStatic = r.nextBoolean();
				long thread = r.nextLong();
				Field f = new Field(pakkage, clazz, name, id, isStatic, null);
				if (!f.equals(current)) {
					current = f;
					currentCov = new FieldCoverage(f);
					fields.add(currentCov);
				}
				currentCov.getThreadsSeen().add(thread);
			}
			return fields;
		}
	}

}
