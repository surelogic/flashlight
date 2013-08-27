package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.Query;
import com.surelogic.common.jdbc.Queryable;
import com.surelogic.common.jdbc.Result;
import com.surelogic.common.jdbc.ResultHandler;
import com.surelogic.common.jdbc.Row;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;

public class ThreadNamePostPrep implements IPostPrep, ResultHandler<Void> {

    Query q;

    @Override
    public String getDescription() {
        return "\"Uniquify\" thread names.";
    }

    @Override
    public void doPostPrep(Connection c, SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        q = new ConnectionQuery(c);
        q.prepared("Flashlight.ThreadNamePostPrep.selectThreads", this).call();
    }

    @Override
    public Void handle(Result result) {
        Queryable<Void> updateThreadName = q
                .prepared("Flashlight.ThreadNamePostPrep.updateThreadNames");
        int counter = 0;
        String curName = null;
        for (Row r : result) {
            long threadId = r.nextLong();
            String nextName = r.nextString();
            if (!nextName.equals(curName)) {
                curName = nextName;
                counter = 1;
            } else {
                updateThreadName.call(nextName + "(" + counter++ + ")",
                        threadId);
            }
        }
        return null;
    }

}
