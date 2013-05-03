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
import java.util.List;

import com.surelogic.common.jdbc.ConnectionQuery;
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

public class HappensBeforePostPrep implements IPostPrep {

    private ConnectionQuery q;
    private HappensBeforeAnalysis hb;
    private Queryable<Void> badHappensBefore;

    @Override
    public String getDescription() {
        return "Checking happens-before relationships.";
    }

    @Override
    public void doPostPrep(Connection c, final SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        q = new ConnectionQuery(c);
        // First we build out our HappensBeforeVolatile table
        q.statement("Accesses.prep.volatileWrites").call();
        c.commit();
        q.statement("Accesses.prep.volatileReads").call();
        c.commit();
        try {
            addConstraints(c, schema, "add_volatile_constraints.sql");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error reading volatile happens before constraints.", e);
        }
        hb = new HappensBeforeAnalysis(c);
        badHappensBefore = q.prepared("Accesses.prep.insertBadHappensBefore");
        q.statement("Accesses.prep.selectStatics", new StaticHandler()).call();
        q.statement("Accesses.prep.selectFields", new InstanceHandler()).call();
        c.commit();
        try {
            addConstraints(c, schema, "add_badhappensbefore_constraints.sql");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error reading bad happens before constraints.", e);
        }
        hb.finished();
    }

    void addConstraints(Connection c, SchemaData data, String res)
            throws IOException, SQLException {
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

    private class InstanceHandler extends NullRowHandler {
        final Queryable<Boolean> check = q.prepared(
                "Accesses.prep.selectInstanceField", new AccessHandler());
        final Queryable<?> recordBlocks = q.prepared(
                "Accesses.prep.selectInstanceField", new BlockStatsHandler());

        long field;
        long receiver;

        @Override
        protected void doHandle(Row r) {
            field = r.nextLong();
            receiver = r.nextLong();
            recordBlocks.call(field, receiver);
            if (!check.call(field, receiver)) {
                badHappensBefore.call(field, receiver);
            }
        }

        private class BlockStatsHandler extends NullRowHandler {

            Timestamp beginThread, endThread;
            int reads, writes;
            long lastThread = -1;

            private final Queryable<?> insertBlockStats = q
                    .prepared("Accesses.prep.insertBlockStats");
            private final Queryable<?> insertFieldBlockStats = q
                    .prepared("Accesses.prep.insertFieldBlockStats");

            private final Queryable<?> countInterleavingFields = q.prepared(
                    "Accesses.prep.interleavingFields",
                    new InterleavingFieldHandler());

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
                    countInterleavingFields.call(receiver, beginThread,
                            endThread);

                }
            }

            private class InterleavingFieldHandler extends NullResultHandler {

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
                        inThread = rThread == lastThread;
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
                            fields.forEachValue(new TObjectProcedure<Interleaving>() {

                                @Override
                                public boolean execute(Interleaving i) {
                                    i.inThread = true;
                                    return true;
                                }
                            });
                        }
                    }
                    insertBlockStats.call(field, receiver, lastThread,
                            beginThread, endThread, reads, writes,
                            interleavings * 100 / (reads + writes));
                    fields.forEachEntry(new TLongObjectProcedure<Interleaving>() {

                        @Override
                        public boolean execute(long rField, Interleaving i) {
                            insertFieldBlockStats.call(field, rField, receiver,
                                    lastThread, beginThread, endThread, reads,
                                    writes, i.interleavings * 100
                                            / (reads + writes));
                            return true;
                        }
                    });
                }

            }

        }

    }

    class Interleaving {
        long interleavings = 0;
        boolean inThread = true;
    }

    private class StaticHandler extends NullRowHandler {
        final Queryable<Boolean> check = q.prepared(
                "Accesses.prep.selectStaticField", new AccessHandler());

        @Override
        protected void doHandle(Row r) {
            long field = r.nextLong();
            if (!check.call(field)) {
                badHappensBefore.call(field, Nulls.LONG);
            }
        }

    }

    private class AccessHandler implements ResultHandler<Boolean> {

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
                        if (!hb.hasHappensBefore(lastWrite, lastWriteThread,
                                ts, thread)) {
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
