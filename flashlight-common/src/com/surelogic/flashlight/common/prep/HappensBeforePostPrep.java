package com.surelogic.flashlight.common.prep;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import com.surelogic.common.jdbc.ConnectionQuery;
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

            private final Queryable<Void> insertBlockStats = q
                    .prepared("Accesses.prep.insertBlockStats");
            private final Queryable<Integer> countInterleavingFields = q
                    .prepared("Accesses.prep.interleavingFields",
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
                int total = reads + writes;
                if (total > 20) {
                    double leaves = countInterleavingFields.call(receiver,
                            beginThread, endThread);
                    // (FIELD, RECEIVER, INTHREAD, START, STOP, READS, WRITES,
                    // QUOTIENT)
                    insertBlockStats.call(field, receiver, lastThread,
                            beginThread, endThread, reads, writes, leaves * 100
                                    / total);
                }
            }

            private class InterleavingFieldHandler implements
                    ResultHandler<Integer> {

                @Override
                public Integer handle(Result result) {
                    boolean inThread = true;
                    int interleavings = 0;
                    for (Row r : result) {
                        long thread = r.nextLong();
                        if (thread != lastThread && inThread) {
                            interleavings++;
                        }
                        inThread = thread == lastThread;
                    }
                    return interleavings;
                }

            }

        }

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
