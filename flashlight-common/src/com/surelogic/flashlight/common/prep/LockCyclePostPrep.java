package com.surelogic.flashlight.common.prep;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.stack.array.TLongArrayStack;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.NullRowHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;

public class LockCyclePostPrep implements IPostPrep {

    private ConnectionQuery q;
    TLongObjectHashMap<List<Edge>> graph;

    @Override
    public String getDescription() {
        return "Running lock cycle analysis.";
    }

    @Override
    public void doPostPrep(Connection c, SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        q = new ConnectionQuery(c);
        q.statement("LockCycles.edges", new EdgeHandler()).call();
    }

    class EdgeHandler extends NullRowHandler {
        TLongObjectHashMap<TLongArrayStack> threads;

        @Override
        protected void doHandle(Row r) {
            long lock = r.nextLong();
            long thread = r.nextLong();
            List<Edge> list = graph.get(lock);
            if (list == null) {
                graph.put(lock, new ArrayList<LockCyclePostPrep.Edge>(3));
            }
        }

    }

    static class Edge {
        long held;
        long acquired;
        TLongSet threads;
    }

}
