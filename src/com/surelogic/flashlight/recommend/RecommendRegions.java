package com.surelogic.flashlight.recommend;

import gnu.trove.TLongArrayList;
import gnu.trove.TLongHashSet;
import gnu.trove.TLongProcedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.NullResultHandler;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.flashlight.schema.Trace;

public final class RecommendRegions {

	public static DBQuery<List<RecommendedRegion>> recommendedFieldLockRegions() {
		return new DBQuery<List<RecommendedRegion>>() {

			public List<RecommendedRegion> perform(final Query q) {
				return q.prepared("Flashlight.Region.lockIsField",
						new RecommendedRegionHandler(q)).call();
			}
		};
	}

	/**
	 * This query lists all of the static regions protected by the class they
	 * are declared in that can be inferred from the given Flashlight run.
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<List<RecommendedRegion>> lockIsClassRegions() {
		return new DBQuery<List<RecommendedRegion>>() {

			public List<RecommendedRegion> perform(final Query q) {
				return q.prepared("Flashlight.Region.lockIsClass",
						new RecommendedRegionHandler(q)).call();
			}
		};
	}

	/**
	 * This query lists all of the regions protected by a <code>this</code> that
	 * can be inferred from the given Flashlight run.
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<List<RecommendedRegion>> lockIsThisRegions() {
		return new DBQuery<List<RecommendedRegion>>() {

			public List<RecommendedRegion> perform(final Query q) {
				return q.prepared("Flashlight.Region.lockIsThis",
						new RecommendedRegionHandler(q)).call();
			}
		};
	}

	/**
	 * 
	 * 
	 * @return a list of proposed regions
	 */
	public static DBQuery<Map<String, List<RecommendedRegion>>> observedRegions() {
		return new DBQuery<Map<String, List<RecommendedRegion>>>() {

			public Map<String, List<RecommendedRegion>> perform(final Query q) {
				final Map<String, List<RecommendedRegion>> regionMap = new HashMap<String, List<RecommendedRegion>>();
				q.prepared("Flashlight.Region.observed",
						new NullResultHandler() {
							@Override
							protected void doHandle(final Result result) {
								RecommendedRegion region = null;
								String curS = "Y";
								for (final Row r : result) {
									final String e = r.nextString();
									List<RecommendedRegion> regions = regionMap
											.get(e);
									if (regions == null) {
										regions = new ArrayList<RecommendedRegion>();
										regionMap.put(e, regions);
										region = null;
									}
									final String p = r.nextString();
									final String c = r.nextString();
									final String f = r.nextString();
									final int viz = r.nextInt();
									final boolean isS = r.nextBoolean();
									final boolean isF = r.nextBoolean();
									final boolean isV = r.nextBoolean();
									final String s = r.nextString();
									if (region == null
											|| !p.equals(region.getPackage())
											|| !c.equals(region.getClazz())
											|| !s.equals(curS)) {
										region = new RecommendedRegion(p, c, s
												.equals("Y"));
										curS = s;
										regions.add(region);
									}
									region.addField(new Field(f, Visibility
											.fromFlag(viz), isS, isF, isV));
								}
							}
						}).call();
				return regionMap;
			}
		};
	}

	/**
	 * Produces a list of recommended regions from the result of a query. The
	 * query result should contain the following columns in order:
	 * <ol>
	 * <li>Package Name</li>
	 * <li>Class Name</li>
	 * <li>Lock Name</li>
	 * <li>Lock Identifier</li>
	 * <li>Field Name</li>
	 * <li>Field Identifier</li>
	 * </ol>
	 * 
	 * @author nathan
	 * 
	 */
	private static class RecommendedRegionHandler implements
			ResultHandler<List<RecommendedRegion>> {
		private final Query q;

		RecommendedRegionHandler(final Query q) {
			this.q = q;
		}

		public List<RecommendedRegion> handle(final Result result) {
			final List<RecommendedRegion> regions = new ArrayList<RecommendedRegion>();
			RecommendedRegion region = null;
			TLongHashSet ls = null;
			TLongHashSet fs = null;
			for (final Row r : result) {
				final String p = r.nextString();
				final String c = r.nextString();
				final String l = r.nextString();
				final long lid = r.nextLong();
				final String f = r.nextString();
				final long fid = r.nextLong();
				final int viz = r.nextInt();
				final boolean isS = r.nextBoolean();
				final boolean isF = r.nextBoolean();
				final boolean isV = r.nextBoolean();
				if (region == null || !p.equals(region.getPackage())
						|| !c.equals(region.getClazz())
						|| !l.equals(region.getLock())) {
					if (region != null) {
						region.getRequiresLockMethods().addAll(
								traces(ls, fs).perform(q));
					}
					fs = new TLongHashSet();
					ls = new TLongHashSet();
					region = new RecommendedRegion(p, c, l);
					regions.add(region);
				}
				region.addField(new Field(f, Visibility.fromFlag(viz), isS,
						isF, isV));
				fs.add(fid);
				ls.add(lid);
			}
			return regions;

		}
	}

	/**
	 * This query takes a lock trace and a set of field traces. It computes from
	 * this the set of methods that access the fields, but do not explicitly get
	 * the lock.
	 * 
	 * @param traceId
	 * @param fieldId
	 * @return
	 */
	private static DBQuery<Set<Method>> traces(final TLongHashSet locks,
			final TLongHashSet fields) {
		return new DBQuery<Set<Method>>() {
			public Set<Method> perform(final Query q) {
				final long ms = System.currentTimeMillis();
				final Set<Method> methods = new HashSet<Method>();
				/*
				 * The trace algorithm works like this.
				 * 
				 * 1. compute the set of stack traces for all acquisitions of
				 * the lock
				 * 
				 * 2. compute the set of stack traces for all acquisitions of
				 * the set of fields
				 * 
				 * 3. for each field trace, find the best fit from the lock
				 * traces
				 * 
				 * 4. then add the field trace elements that don't fit to our
				 * set of methods that require a lock
				 */
				final Queryable<long[]> lockTracesQ = q
						.prepared("Flashlight.Region.lockTraces",
								new LongResultHandler());
				final TLongHashSet lockTraceIds = new TLongHashSet();
				locks.forEach(new TLongProcedure() {
					public boolean execute(final long lockId) {
						lockTraceIds.addAll(lockTracesQ.call(lockId));
						return true;
					}
				});

				final List<List<Trace>> lockTraces = new ArrayList<List<Trace>>();
				lockTraceIds.forEach(new TLongProcedure() {
					public boolean execute(final long l) {
						final List<Trace> lockTrace = Trace.stackTrace(l)
								.perform(q);
						Collections.reverse(lockTrace);
						lockTraces.add(lockTrace);
						return true;
					}
				});

				final Queryable<long[]> fieldTracesQ = q.prepared(
						"Flashlight.Region.fieldTraces",
						new LongResultHandler());
				final TLongHashSet fieldTraceIds = new TLongHashSet();
				fields.forEach(new TLongProcedure() {
					public boolean execute(final long fieldId) {
						fieldTraceIds.addAll(fieldTracesQ.call(fieldId));
						return true;
					}
				});

				final List<List<Trace>> fieldTraces = new ArrayList<List<Trace>>();
				fieldTraceIds.forEach(new TLongProcedure() {
					public boolean execute(final long f) {
						final List<Trace> fieldTrace = Trace.stackTrace(f)
								.perform(q);
						Collections.reverse(fieldTrace);
						fieldTraces.add(fieldTrace);
						return true;
					}
				});
				System.out.println(String.format("Locks: %s\nFields: %s",
						lockTraces.size(), fieldTraces.size()));
				for (final List<Trace> fieldTrace : fieldTraces) {
					int bestFit = 0;
					for (final List<Trace> lockTrace : lockTraces) {
						int fit = 0;
						final Iterator<Trace> fIter = fieldTrace.iterator();
						final Iterator<Trace> lIter = lockTrace.iterator();
						while (fIter.hasNext() && lIter.hasNext()
								&& traceEquals(fIter.next(), lIter.next())) {
							fit++;
						}
						bestFit = Math.max(fit, bestFit);
					}
					for (final Trace t : fieldTrace.subList(bestFit, fieldTrace
							.size())) {
						methods.add(new Method(t.getPackage(), t.getClazz(), t
								.getLoc()));
					}
				}
				final long newMs = System.currentTimeMillis();
				System.out.println(String.format("Time: %dms\n", newMs - ms));
				return methods;
			}
		};
	}

	private static final boolean traceEquals(final Trace one, final Trace two) {
		return one.getPackage().equals(two.getPackage())
				&& one.getClazz().equals(two.getClazz())
				&& one.getLoc().equals(two.getLoc());
	}

	/**
	 * Produces a long array.
	 */
	private static class LongResultHandler implements ResultHandler<long[]> {

		public long[] handle(final Result result) {
			final TLongArrayList lst = new TLongArrayList();
			for (final Row r : result) {
				lst.add(r.nextLong());
			}
			return lst.toNativeArray();
		}
	}
}
