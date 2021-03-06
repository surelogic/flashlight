package com.surelogic.flashlight.recommend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.procedures.LongProcedure;
import com.surelogic.common.derby.sqlfunctions.Trace;
import com.surelogic.common.jdbc.DBQuery;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;

public final class RecommendRegions {

  public static DBQuery<List<RecommendedRegion>> recommendedFieldLockRegions() {
    return new DBQuery<List<RecommendedRegion>>() {

      @Override
      public List<RecommendedRegion> perform(final Query q) {
        return q.prepared("Flashlight.Region.lockIsField", new RecommendedRegionHandler(q)).call();
      }
    };
  }

  /**
   * This query lists all of the static regions protected by the class they are
   * declared in that can be inferred from the given Flashlight run.
   * 
   * @return a list of proposed regions
   */
  public static DBQuery<List<RecommendedRegion>> lockIsClassRegions() {
    return new DBQuery<List<RecommendedRegion>>() {

      @Override
      public List<RecommendedRegion> perform(final Query q) {
        return q.prepared("Flashlight.Region.lockIsClass", new RecommendedRegionHandler(q)).call();
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

      @Override
      public List<RecommendedRegion> perform(final Query q) {
        return q.prepared("Flashlight.Region.lockIsThis", new RecommendedRegionHandler(q)).call();
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
  private static class RecommendedRegionHandler implements ResultHandler<List<RecommendedRegion>> {
    private final Query q;

    RecommendedRegionHandler(final Query q) {
      this.q = q;
    }

    @Override
    public List<RecommendedRegion> handle(final Result result) {
      final List<RecommendedRegion> regions = new ArrayList<>();
      RecommendedRegion region = null;
      LongSet ls = null;
      LongSet fs = null;
      for (final Row r : result) {
        final String p = r.nextString();
        final String c = r.nextString();
        final String l = r.nextString();
        final long lid = r.nextLong();
        final boolean lIsS = r.nextBoolean();
        final String f = r.nextString();
        final long fid = r.nextLong();
        final int viz = r.nextInt();
        final boolean isS = r.nextBoolean();
        final boolean isF = r.nextBoolean();
        final boolean isV = r.nextBoolean();
        final boolean isA = r.nextBoolean();
        if (region == null || !p.equals(region.getPackage()) || !c.equals(region.getClazz()) || !l.equals(region.getLock())) {
          if (region != null) {
            // FIXME performance
            region.getRequiresLockMethods().addAll(traces(ls, fs).perform(q));
          }
          fs = new LongScatterSet();
          ls = new LongScatterSet();
          region = new RecommendedRegion(p, c, l, lIsS);
          regions.add(region);
        }
        region.addField(new FieldLoc(f, Visibility.fromFlag(viz), isS, isF, isV, isA));
        fs.add(fid);
        ls.add(lid);
      }
      // Finally, add the requires lock methods for the last region
      if (region != null) {
        region.getRequiresLockMethods().addAll(traces(ls, fs).perform(q));
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
  static DBQuery<Set<MethodLoc>> traces(final LongSet locks, final LongSet fields) {
    return new DBQuery<Set<MethodLoc>>() {
      @Override
      public Set<MethodLoc> perform(final Query q) {
        final long ms = System.currentTimeMillis();
        final Set<MethodLoc> methods = new HashSet<>();
        /*
         * The trace algorithm works like this.
         * 
         * 1. compute the set of stack traces for all acquisitions of the lock
         * 
         * 2. compute the set of stack traces for all acquisitions of the set of
         * fields
         * 
         * 3. for each field trace, find the best fit from the lock traces
         * 
         * 4. then add the field trace elements that don't fit to our set of
         * methods that require a lock
         */
        final Queryable<long[]> lockTracesQ = q.prepared("Flashlight.Region.lockTraces", new LongResultHandler());
        final LongSet lockTraceIds = new LongScatterSet();
        locks.forEach(new LongProcedure() {
          public void apply(final long lockId) {
            for (long l : lockTracesQ.call(lockId)) {
              lockTraceIds.add(l);
            }
          }
        });
        final List<List<Trace>> lockTraces = new ArrayList<>();
        lockTraceIds.forEach(new LongProcedure() {
          public void apply(final long l) {
            final List<Trace> lockTrace = Trace.stackTrace(l).perform(q);
            Collections.reverse(lockTrace);
            lockTraces.add(lockTrace);
          }
        });

        final Queryable<long[]> fieldTracesQ = q.prepared("Flashlight.Region.fieldTraces", new LongResultHandler());
        final LongHashSet fieldTraceIds = new LongScatterSet();
        fields.forEach(new LongProcedure() {
          public void apply(final long fieldId) {
            fieldTraceIds.addAll(fieldTracesQ.call(fieldId));
          }
        });

        final List<List<Trace>> fieldTraces = new ArrayList<>();
        fieldTraceIds.forEach(new LongProcedure() {
          public void apply(final long f) {
            final List<Trace> fieldTrace = Trace.stackTrace(f).perform(q);
            Collections.reverse(fieldTrace);
            fieldTraces.add(fieldTrace);
          }
        });
        System.out.println(String.format("Locks: %s\nFields: %s", lockTraces.size(), fieldTraces.size()));
        for (final List<Trace> fieldTrace : fieldTraces) {
          int bestFit = 0;
          for (final List<Trace> lockTrace : lockTraces) {
            int fit = 0;
            final Iterator<Trace> fIter = fieldTrace.iterator();
            final Iterator<Trace> lIter = lockTrace.iterator();
            while (fIter.hasNext() && lIter.hasNext() && traceEquals(fIter.next(), lIter.next())) {
              fit++;
            }
            bestFit = Math.max(fit, bestFit);
          }
          for (final Trace t : fieldTrace.subList(bestFit, fieldTrace.size())) {
            methods.add(new MethodLoc(t.getPackage(), t.getClazz(), t.getLoc()));
          }
        }
        final long newMs = System.currentTimeMillis();
        System.out.println(String.format("Time: %dms\n", newMs - ms));
        return methods;
      }
    };
  }

  static final boolean traceEquals(final Trace one, final Trace two) {
    return one.getPackage().equals(two.getPackage()) && one.getClazz().equals(two.getClazz()) && one.getLoc().equals(two.getLoc());
  }

  /**
   * Produces a long array.
   */
  static class LongResultHandler implements ResultHandler<long[]> {

    @Override
    public long[] handle(final Result result) {
      final LongArrayList lst = new LongArrayList();
      for (final Row r : result) {
        lst.add(r.nextLong());
      }
      return lst.toArray();
    }
  }
}
