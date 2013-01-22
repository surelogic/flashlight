package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Nulls;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.flashlight.common.HappensBeforeAnalysis;

public class HappensBeforePostPrep implements IPostPrep {

    ConnectionQuery q;
    HappensBeforeAnalysis hb;
    private Queryable<Void> badHappensBefore;

    @Override
    public String getDescription() {
        return "Checking happens-before relationships.";
    }

    @Override
    public void doPostPrep(Connection c, SLProgressMonitor mon)
            throws SQLException {
        q = new ConnectionQuery(c);
        hb = new HappensBeforeAnalysis(c);
        // First we build out our HappensBeforeVolatile table

        badHappensBefore = q.prepared("Accesses.prep.insertBadHappensBefore");
        q.statement("Accesses.prep.selectStatics", new StaticHandler()).call();
        q.statement("Accesses.prep.selectFields", new InstanceHandler()).call();
    }

    private class InstanceHandler extends NullRowHandler {
        final Queryable<Boolean> check = q.prepared(
                "Accesses.prep.selectInstanceField", new AccessHandler());

        @Override
        protected void doHandle(Row r) {
            long field = r.nextLong();
            long receiver = r.nextLong();
            if (!check.call(field, receiver)) {
                badHappensBefore.call(field, receiver);
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

    // SELECT INTHREAD,TS,RW
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
