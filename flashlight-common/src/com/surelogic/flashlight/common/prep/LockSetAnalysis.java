package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.carrotsearch.hppc.LongObjectMap;
import com.carrotsearch.hppc.LongObjectScatterMap;
import com.carrotsearch.hppc.procedures.LongObjectProcedure;
import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.NullResultHandler;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Nulls;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.flashlight.common.LockId;
import com.surelogic.flashlight.common.LockType;

/**
 * The lock set analysis looks for bad field publishes during construction of an
 * object and for field access that hold an inconsistent set of locks.
 */
public class LockSetAnalysis implements IPostPrep {

  final Logger log = SLLogger.getLoggerFor(LockSetAnalysis.class);

  Connection c;

  @Override
  public String getDescription() {
    return "Performing lock set analysis";
  }

  void commit() {
    try {
      c.commit();
    } catch (final SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void doPostPrep(final Connection c, final SchemaData schema, final SLProgressMonitor mon) {
    this.c = c;
    doPerform(new ConnectionQuery(c), mon);
  }

  public void doPerform(final Query q, final SLProgressMonitor mon) {
    log.fine("Inserting lock cycle thread info");
    q.statement("Deadlock.lockCycleThreads").call();
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
      Queryable<Void> insert = q.prepared("LockSet.v2.insertInterestingField");

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

      void handleAccesses(final LockSets sets, final Result indirectAccesses, final Result accesses) {
        final Iterator<Row> iaRow = indirectAccesses.iterator();
        final Iterator<Row> aRow = accesses.iterator();
        Access a = null;
        IndirectAccess ia = null;
        int count = 0;
        while (aRow.hasNext() || iaRow.hasNext()) {
          if (++count % 10000 == 0) {
            if (mon.isCanceled()) {
              return;
            } else {
              commit();
            }
          }
          if (a == null && aRow.hasNext()) {
            a = new Access(aRow.next());
          }
          if (ia == null && iaRow.hasNext()) {
            ia = new IndirectAccess(iaRow.next());
          }
          if (a == null) {
            sets.indirectAccess(ia);
            ia = null;
          } else if (ia == null || a.ts.before(ia.ts)) {
            sets.access(a);
            a = null;
          } else {
            sets.indirectAccess(ia);
            ia = null;
          }
        }
        if (mon.isCanceled()) {
          return;
        }
        log.fine("Writing out locking statistics.");
        sets.writeStatistics(q);
        commit();
        if (mon.isCanceled()) {
          return;
        }
        // Add foreign key to ACCESSLOCKSHELD table
        log.fine("Inserting shared fields.");
        q.prepared("LockSet.v2.sharedFields", new NullRowHandler() {
          Queryable<Void> insert = q.prepared("LockSet.v2.insertSharedField");

          @Override
          protected void doHandle(final Row r) {
            insert.call(r.nextLong(), r.nextLong());
          }
        }).call();
      }

      @Override
      public void doHandle(final Result lockDurations) {
        final LockSets sets = new LockSets(lockDurations, q.prepared("LockSet.v2.updateAccessLocksHeld"),
            q.prepared("LockSet.v2.updateIndirectAccessLocksHeld"));
        q.prepared("LockSet.v2.indirectAccesses", new NullResultHandler() {
          @Override
          protected void doHandle(final Result indirectAccesses) {
            q.prepared("LockSet.v2.accesses", new NullResultHandler() {
              @Override
              public void doHandle(final Result accesses) {
                handleAccesses(sets, indirectAccesses, accesses);
              }
            }).call();
          }
        }).call();

      }
    }).call();
    commit();
    log.fine("Adding locking statistic constraints");
    q.prepared("LockSet.v2.accessLocksHeldConstraint").call();
    q.prepared("LockSet.v2.accessLockAcquisitionConstraint").call();
    q.prepared("LockSet.v2.indirectAccessLocksHeldConstraint").call();
    q.prepared("LockSet.v2.indirectAccessLockAcquisitionConstraint").call();
  }

  static class IndirectAccess {
    final long id;
    final Timestamp ts;
    final long thread;
    final long receiver;

    public IndirectAccess(final Row r) {
      id = r.nextLong();
      ts = r.nextTimestamp();
      thread = r.nextLong();
      receiver = r.nextLong();
    }
  }

  static class Access {
    final long id;
    final Timestamp ts;
    final long thread;
    final long field;
    final Long receiver;
    final boolean read;
    final boolean underConstruction;

    public Access(final Row r) {
      id = r.nextLong();
      ts = r.nextTimestamp();
      thread = r.nextLong();
      field = r.nextLong();
      receiver = r.nullableLong();
      read = "R".equals(r.nextString());
      underConstruction = "Y".equals(r.nextString());
    }
  }

  private class LockSets {

    private final LongObjectMap<Set<LockId>> fields = new LongObjectScatterMap<>();
    private final LongObjectMap<LongObjectMap<Set<LockId>>> instances = new LongObjectScatterMap<>();
    private final Map<StaticInstance, StaticCount> staticCounts = new HashMap<>();
    private final Map<FieldInstance, Count> counts = new HashMap<>();
    final ThreadLocks locks;
    final Queryable<?> updateAccess;
    final Queryable<?> updateIndirectAccess;

    public LockSets(final Result lockDurations, final Queryable<?> updateAccess, final Queryable<?> updateIndirectAccess) {
      locks = new ThreadLocks(lockDurations);
      this.updateAccess = updateAccess;
      this.updateIndirectAccess = updateIndirectAccess;
    }

    public void writeStatistics(final Query q) {
      final Queryable<Void> insertFieldLockSets = q.prepared("LockSet.v2.insertFieldLockSets");
      final Queryable<Void> insertInstanceLockSets = q.prepared("LockSet.v2.insertInstanceLockSets");
      log.fine("Static field lock sets.");
      fields.forEach(new LongObjectProcedure<Set<LockId>>() {
        int count = 0;

        public void apply(long field, Set<LockId> locks) {
          for (final Iterator<LockId> it = locks.iterator(); it.hasNext();) {
            LockId lock = it.next();
            insertFieldLockSets.call(field, lock.getId(), lock.getType().getFlag());
            if (++count % 10000 == 0) {
              commit();
            }
          }
        }
      });
      log.fine("Instance lock sets.");
      instances.forEach(new LongObjectProcedure<LongObjectMap<Set<LockId>>>() {
        int count;

        public void apply(final long field, LongObjectMap<Set<LockId>> instance) {
          final Set<LockId> fieldSet = new HashSet<>();
          instance.forEach(new LongObjectProcedure<Set<LockId>>() {
            boolean first = true;

            public void apply(long receiver, Set<LockId> instanceSet) {
              if (first) {
                fieldSet.addAll(instanceSet);
                first = false;
              } else {
                fieldSet.retainAll(instanceSet);
              }
              for (final Iterator<LockId> it = instanceSet.iterator(); it.hasNext();) {
                LockId lock = it.next();
                insertInstanceLockSets.call(field, receiver, lock.getId(), lock.getType().getFlag());
              }
              if (++count % 10000 == 0) {
                commit();
              }
            }
          });
          for (final Iterator<LockId> it = fieldSet.iterator(); it.hasNext();) {
            LockId lock = it.next();
            insertFieldLockSets.call(field, lock.getId(), lock.getType().getFlag());
            if (++count % 10000 == 0) {
              commit();
            }
          }
        }
      });

      final Queryable<Void> insertStaticCounts = q.prepared("LockSet.v2.insertStaticCounts");
      final Queryable<Void> insertFieldCounts = q.prepared("LockSet.v2.insertFieldCounts");
      int count = 0;
      log.fine("Locking counts.");
      for (final Entry<StaticInstance, StaticCount> e : staticCounts.entrySet()) {
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
        insertFieldCounts.call(fi.thread, fi.field, fi.receiver, c.read, c.write, c.readUC, c.writeUC);
        if (++count % 10000 == 0) {
          commit();
        }
      }
    }

    public void access(final Access a) {
      if (a.receiver == null) {
        if (a.underConstruction) {
          classUnderConstruction(a.id, a.ts, a.thread, a.field, a.read);
        } else {
          staticAccess(a.id, a.ts, a.thread, a.field, a.read);
        }
      } else {
        if (a.underConstruction) {
          instanceUnderConstruction(a.id, a.ts, a.thread, a.field, a.receiver, a.read);
        } else {
          instanceAccess(a.id, a.ts, a.thread, a.field, a.receiver, a.read);
        }
      }
    }

    public void indirectAccess(final IndirectAccess ia) {
      locks.ensureTime(ia.ts);
      final List<LockId> lockSet = locks.getLocks(ia.thread);
      updateIndirectAccess.call(ia.id, lockSet.size(), Nulls.coerce(locks.getLastAcquisition(ia.thread)));
    }

    void staticAccess(final long id, final Timestamp ts, final long thread, final long field, final boolean read) {
      locks.ensureTime(ts);
      Set<LockId> fieldSet = fields.get(field);
      final List<LockId> lockSet = locks.getLocks(thread);
      updateAccess.call(id, lockSet.size(), Nulls.coerce(locks.getLastAcquisition(thread)));
      if (fieldSet == null) {
        fieldSet = new HashSet<>(lockSet.size());
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

    void classUnderConstruction(final long id, final Timestamp ts, final long thread, final long field, final boolean read) {
      locks.ensureTime(ts);
      final List<LockId> lockSet = locks.getLocks(thread);
      updateAccess.call(id, lockSet.size(), Nulls.coerce(locks.getLastAcquisition(thread)));
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

    void instanceUnderConstruction(final long id, final Timestamp ts, final long thread, final long field, final Long receiver,
        final boolean read) {
      locks.ensureTime(ts);
      updateAccess.call(id, locks.getLocks(thread).size(), Nulls.coerce(locks.getLastAcquisition(thread)));
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

    void instanceAccess(final long id, final Timestamp ts, final long thread, final long field, final long receiver,
        final boolean read) {
      locks.ensureTime(ts);
      LongObjectMap<Set<LockId>> fieldMap = instances.get(field);
      if (fieldMap == null) {
        fieldMap = new LongObjectScatterMap<>();
        instances.put(field, fieldMap);
      }
      Set<LockId> instance = fieldMap.get(receiver);
      final List<LockId> lockSet = locks.getLocks(thread);
      updateAccess.call(id, lockSet.size(), Nulls.coerce(locks.getLastAcquisition(thread)));
      if (instance == null) {
        instance = new HashSet<>();
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
      result = prime * result + (int) (field ^ field >>> 32);
      result = prime * result + (int) (thread ^ thread >>> 32);
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

    public FieldInstance(final long thread, final long field, final long receiver) {
      super();
      this.thread = thread;
      this.field = field;
      this.receiver = receiver;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (field ^ field >>> 32);
      result = prime * result + (int) (receiver ^ receiver >>> 32);
      result = prime * result + (int) (thread ^ thread >>> 32);
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

  static class Count {
    long read;
    long write;
    long readUC;
    long writeUC;
  }

  static class StaticCount {
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
    private final LongObjectMap<TreeSet<Lock>> threads;
    private final TreeSet<Lock> activeLocks;
    private Lock lock;

    ThreadLocks(final Result lockDurations) {
      locks = lockDurations.iterator();
      activeLocks = new TreeSet<>(new Comparator<Lock>() {
        @Override
        public int compare(final Lock o1, final Lock o2) {
          return o1.end.compareTo(o2.end);
        }
      });
      threads = new LongObjectScatterMap<>();
    }

    /**
     * Get the collection of locks a thread currently holds.
     * 
     * @param thread
     * @return
     */
    public List<LockId> getLocks(final long thread) {
      final Collection<Lock> set = getThreadSet(thread);
      final List<LockId> locks = new ArrayList<>(set.size());
      for (final Lock l : set) {
        locks.add(l.lock);
      }
      return locks;
    }

    /**
     * Get the lock event id of the last lock acquisition made in this thread
     * before the given time.
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
      for (final Iterator<Lock> li = activeLocks.iterator(); li.hasNext() && (oldLock = li.next()).end.before(time);) {
        getThreadSet(oldLock.thread).remove(oldLock);
        li.remove();
      }
      // Check to see if we already have one, and if so, do we need to
      // get more
      if (lock != null) {
        if (!lock.start.after(time)) {
          if (!lock.end.before(time)) {
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
        if (!l.start.after(time)) {
          if (!l.end.before(time)) {
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
        threadSet = new TreeSet<>(new Comparator<Lock>() {
          @Override
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
    final LockId lock;
    final long startEvent;
    final Timestamp start;
    final Timestamp end;

    Lock(final Row row) {
      thread = row.nextLong();
      lock = new LockId(row.nextLong(), LockType.fromFlag(row.nextString()));
      start = row.nextTimestamp();
      end = row.nextTimestamp();
      startEvent = row.nextLong();
    }

  }
}
