package com.surelogic.flashlight.common.prep;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TObjectProcedure;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.surelogic.common.jdbc.BooleanResultHandler;
import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.LongResultHandler;
import com.surelogic.common.jdbc.NullResultHandler;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Nulls;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jdbc.SchemaUtility;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.HappensBeforeAnalysis;
import com.surelogic.flashlight.common.prep.ClassHierarchy.ClassNode;

public class HappensBeforePostPrep implements IPostPrep {

  final ClassHierarchy ch;
  ConnectionQuery q;
  HappensBeforeAnalysis hb;
  Queryable<Void> badHappensBefore;

  public HappensBeforePostPrep(ClassHierarchy ch) {
    this.ch = ch;
  }

  @Override
  public String getDescription() {
    return "Checking happens-before relationships.";
  }

  @Override
  public void doPostPrep(Connection c, final SchemaData schema, SLProgressMonitor mon) throws SQLException {
    q = new ConnectionQuery(c);
    // First we build out our HappensBeforeVolatile table
    q.statement("Accesses.prep.volatileWrites").call();
    c.commit();
    q.statement("Accesses.prep.volatileReads").call();
    c.commit();
    try {
      addConstraints(c, schema, "add_volatile_constraints.sql");
    } catch (IOException e) {
      throw new IllegalStateException("Error reading volatile happens before constraints.", e);
    }
    // We also need to build the class initialization table
    q.statement("Accesses.prep.selectClassAccesses", new ClassInitHandler()).call();

    hb = new HappensBeforeAnalysis(c);
    badHappensBefore = q.prepared("Accesses.prep.insertBadHappensBefore");
    q.statement("Accesses.prep.selectStatics", new StaticHandler()).call();
    q.statement("Accesses.prep.selectFields", new InstanceHandler()).call();
    c.commit();
    try {
      addConstraints(c, schema, "add_badhappensbefore_constraints.sql");
    } catch (IOException e) {
      throw new IllegalStateException("Error reading bad happens before constraints.", e);
    }
    hb.finished();
  }

  void addConstraints(Connection c, SchemaData data, String res) throws IOException, SQLException {
    final URL script = data.getSchemaResource(res);
    List<StringBuilder> sql = SchemaUtility.getSQLStatements(script);
    Statement st = c.createStatement();
    try {
      for (StringBuilder s : sql) {
        st.execute(s.toString());
        c.commit();
      }
    } finally {
      st.close();
    }
  }

  static class ClassInit {
    StaticAccess end;
    Map<Long, StaticAccess> threads = new HashMap<Long, StaticAccess>();
  }

  private class StaticAccess {
    final long thread;
    final long trace;
    final Timestamp ts;

    public StaticAccess(long thread, long trace, Timestamp ts) {
      this.thread = thread;
      this.trace = trace;
      this.ts = ts;
    }

  }

  class ClassInitHandler extends NullResultHandler {
    final Map<String, ClassInit> classes = new HashMap<String, ClassInit>();
    private final Queryable<Void> insertClassInit = q.prepared("Accesses.prep.insertClassInit");
    private final Queryable<Void> insertClassAccess = q.prepared("Accesses.prep.insertClassAccess");
    private final Queryable<Long> selectClass = q.prepared("Accesses.prep.selectClass", new LongResultHandler());

    ClassInit getClassInit(String name) {
      ClassInit init = classes.get(name);
      if (init == null) {
        init = new ClassInit();
        classes.put(name, init);
      }
      return init;
    }

    private void updateInitEnd(String name, StaticAccess access) {
      ClassInit init = getClassInit(name);
      if (init.end == null || init.end.ts.before(access.ts)) {
        init.end = access;
        for (ClassNode node : ch.getNode(name).getParents()) {
          updateInitEnd(node.getName(), access);
        }
      }
    }

    private void updateThreadAccess(String name, StaticAccess access) {
      ClassInit init = getClassInit(name);
      StaticAccess earliest = init.threads.get(access.thread);
      if (earliest == null || earliest.ts.after(access.ts)) {
        init.threads.put(access.thread, access);
        for (ClassNode node : ch.getNode(name).getParents()) {
          updateThreadAccess(node.getName(), access);
        }
      }
    }

    @Override
    protected void doHandle(Result result) {
      for (Row r : result) {
        long thread = r.nextLong();
        Timestamp ts = r.nextTimestamp();
        long trace = r.nextLong();
        boolean underConstruction = r.nextBoolean();
        String pakkage = r.nextString();
        String clazz = r.nextString();
        String name;
        if (pakkage.equals("(default)")) {
          name = clazz;
        } else {
          name = pakkage + '.' + clazz;
        }
        if (underConstruction) {
          updateInitEnd(name, new StaticAccess(thread, trace, ts));
        } else {
          updateThreadAccess(name, new StaticAccess(thread, trace, ts));
        }
      }

      for (Entry<String, ClassInit> entry : classes.entrySet()) {
        ClassInit init = entry.getValue();
        String fullName = entry.getKey();
        int split = fullName.lastIndexOf('.');
        String pakkage, clazz;
        if (split == -1) {
          pakkage = "(default)";
          clazz = fullName;
        } else {
          pakkage = fullName.substring(0, split);
          clazz = fullName.substring(split + 1);
        }
        Long objId = selectClass.call(pakkage, clazz);
        if (objId != null) {
          StaticAccess end = init.end;
          if (end != null) {
            insertClassInit.call(objId, init.end.thread, init.end.trace, init.end.ts);
            for (StaticAccess a : init.threads.values()) {
              insertClassAccess.call(objId, a.thread, a.trace, a.ts);
            }
          }
        }
      }
    }

  }

  class InstanceHandler extends NullRowHandler {
    final BlockStatsHandler bh = new BlockStatsHandler();
    final Queryable<Boolean> check = q.prepared("Accesses.prep.selectInstanceField", new AccessHandler());
    final Queryable<?> recordBlocks = q.prepared("Accesses.prep.selectInstanceField", bh);

    long field;
    long receiver;

    @Override
    protected void doHandle(Row r) {
      field = r.nextLong();
      receiver = r.nextLong();
      bh.clear();
      recordBlocks.call(field, receiver);
      if (!check.call(field, receiver)) {
        badHappensBefore.call(field, receiver);
      }
    }

    class BlockStatsHandler extends NullRowHandler {

      final InterleavingFieldHandler h = new InterleavingFieldHandler();

      Timestamp beginThread, endThread;
      int reads, writes;
      long lastThread = -1;

      final Queryable<?> insertBlockStats = q.prepared("Accesses.prep.insertBlockStats");
      final Queryable<?> insertFieldBlockStats = q.prepared("Accesses.prep.insertFieldBlockStats");
      final Queryable<Boolean> isFieldLock = q.prepared("Accesses.prep.isFieldLock", new BooleanResultHandler());
      final Queryable<?> countInterleavingFields = q.prepared("Accesses.prep.interleavingFields", h);

      void clear() {
        beginThread = endThread = null;
        reads = writes = 0;
        lastThread = -1;
        h.fields.clear();
      }

      @Override
      protected void doHandle(Row r) {
        long thread = r.nextLong();
        if (thread != lastThread) {
          handleBlock();
          lastThread = thread;
          beginThread = endThread = r.nextTimestamp();
          reads = 0;
          writes = 0;
        } else {
          endThread = r.nextTimestamp();
        }
        if (r.nextString().equals("R")) {
          reads++;
        } else {
          writes++;
        }
      }

      void handleBlock() {
        if (reads + writes > 20) {
          countInterleavingFields.call(receiver, beginThread, endThread);

        }
      }

      class InterleavingFieldHandler extends NullResultHandler {

        TLongObjectMap<Interleaving> fields = new TLongObjectHashMap<Interleaving>();

        @Override
        public void doHandle(Result result) {
          fields.clear();
          boolean inThread = true;
          int interleavings = 0;
          for (Row r : result) {
            long rField = r.nextLong();
            long rThread = r.nextLong();
            // Handle the big block
            if (rThread != lastThread && inThread) {
              interleavings++;
            }
            if (inThread) {
              inThread = rThread == lastThread;
            } else {
              inThread = rThread == lastThread && rField == field;
            }
            // Handle the individual blocks
            if (rField != field) {
              Interleaving i = fields.get(rField);
              if (i == null) {
                i = new Interleaving();
                fields.put(rField, i);
              }
              if (rThread != lastThread && i.inThread) {
                i.interleavings++;
              }
              i.inThread = inThread;
            } else {
              final boolean val = inThread;
              fields.forEachValue(new TObjectProcedure<Interleaving>() {

                @Override
                public boolean execute(Interleaving i) {
                  i.inThread = val;
                  return true;
                }
              });
            }
          }
          if (isFieldLock.call(field, receiver) != Boolean.TRUE) {
            insertBlockStats.call(field, receiver, lastThread, beginThread, endThread, reads, writes, (double) interleavings * 100
                / (reads + writes));
            fields.forEachEntry(new TLongObjectProcedure<Interleaving>() {

              @Override
              public boolean execute(long rField, Interleaving i) {
                if (isFieldLock.call(rField, receiver) != Boolean.TRUE) {
                  insertFieldBlockStats.call(field, rField, receiver, lastThread, beginThread, endThread, reads, writes,
                      (double) i.interleavings * 100 / (reads + writes));
                }
                return true;
              }
            });
          }
        }

      }

    }

  }

  class Interleaving {
    long interleavings = 0;
    boolean inThread = true;
  }

  class StaticHandler extends NullRowHandler {
    final Queryable<Boolean> check = q.prepared("Accesses.prep.selectStaticField", new AccessHandler());

    @Override
    protected void doHandle(Row r) {
      long field = r.nextLong();
      if (!check.call(field)) {
        badHappensBefore.call(field, Nulls.LONG);
      }
    }

  }

  class AccessHandler implements ResultHandler<Boolean> {

    @Override
    public Boolean handle(Result result) {
      Timestamp lastWrite = null;
      long curThread = -1;
      long lastWriteThread = -1;
      for (Row r : result) {
        long thread = r.nextLong();
        Timestamp ts = r.nextTimestamp();
        boolean isRead = r.nextString().equals("R");
        if (!isRead) {
          lastWrite = ts;
          lastWriteThread = thread;
        } else if (thread != curThread) {
          curThread = thread;
          try {
            if (!hb.hasHappensBefore(lastWrite, lastWriteThread, ts, thread)) {
              return false;
            }
          } catch (SQLException e) {
            throw new IllegalStateException(e);
          }
        }
      }
      return true;
    }
  }

}
