package com.surelogic.flashlight.common.prep;

import java.sql.Connection;
import java.sql.SQLException;

import com.surelogic.common.jdbc.ConnectionQuery;
import com.surelogic.common.jdbc.SchemaData;
import com.surelogic.common.jobs.SLProgressMonitor;

public class LockInfoPostPrep implements IPostPrep {

    @Override
    public String getDescription() {
        return "Getting package access counts for locks.";
    }

    @Override
    public void doPostPrep(Connection c, SchemaData schema,
            SLProgressMonitor mon) throws SQLException {
        new ConnectionQuery(c).prepared("Accesses.prep.packageAccessCounts")
                .call();
        new ConnectionQuery(c).prepared("Accesses.prep.lockThreadInfo").call();
    }

}
